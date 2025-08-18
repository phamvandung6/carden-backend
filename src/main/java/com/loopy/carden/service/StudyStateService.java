package com.loopy.carden.service;

import com.loopy.carden.dto.studystate.StudyStateResponseDto;
import com.loopy.carden.entity.Card;
import com.loopy.carden.entity.StudyState;
import com.loopy.carden.entity.StudyState.CardState;
import com.loopy.carden.entity.User;
import org.springframework.data.domain.PageImpl;
import com.loopy.carden.exception.ResourceNotFoundException;
import com.loopy.carden.mapper.StudyStateMapper;
import com.loopy.carden.repository.CardRepository;
import com.loopy.carden.repository.StudyStateRepository;
import com.loopy.carden.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Service implementing Spaced Repetition System (SRS) algorithm
 * Based on enhanced SM-2 (Anki-style) algorithm with learning phases
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class StudyStateService {

    private final StudyStateRepository studyStateRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final StudyStateMapper studyStateMapper;

    // SRS Algorithm Constants
    private static final double INITIAL_EASE_FACTOR = 2.5;
    private static final double MIN_EASE_FACTOR = 1.3;
    private static final double MAX_EASE_FACTOR = 3.0;
    private static final int MAX_INTERVAL_DAYS = 365;
    private static final int LEECH_THRESHOLD = 8;
    
    // Learning phases (in minutes): 1min, 10min, 1day
    private static final List<Integer> LEARNING_STEPS = Arrays.asList(1, 10, 1440);
    private static final double GRADUATION_INTERVAL = 1.0; // days
    private static final double EASY_GRADUATION_INTERVAL = 4.0; // days
    
    // Review multipliers
    private static final double HARD_MULTIPLIER = 1.2;
    private static final double EASY_MULTIPLIER = 1.3;
    private static final double INTERVAL_FUZZ = 0.05; // 5% fuzz to prevent synchronization

    /**
     * Process a card review and update its study state
     * 
     * @param cardId The card being reviewed
     * @param userId The user reviewing the card
     * @param grade The review grade (0-3 scale)
     * @param reviewTime When the review happened
     * @return Updated study state
     */
    public StudyState processReview(Long cardId, Long userId, int grade, LocalDateTime reviewTime) {
        validateGrade(grade);
        
        StudyState studyState = getOrCreateStudyState(cardId, userId);
        StudyState previousState = cloneState(studyState);
        
        // Update review statistics
        updateReviewStatistics(studyState, grade, reviewTime);
        
        // Apply SRS algorithm based on current card state
        if (studyState.getCardState() == CardState.NEW || studyState.isLearning()) {
            processLearningCard(studyState, grade, reviewTime);
        } else {
            processReviewCard(studyState, grade, reviewTime);
        }
        
        // Apply interval fuzz and constraints
        applyIntervalConstraints(studyState);
        
        StudyState result = studyStateRepository.save(studyState);
        
        log.debug("Card {} review processed. Grade: {}, Previous interval: {} days, New interval: {} days, " +
                  "State: {} -> {}", 
                  cardId, grade, 
                  previousState.getIntervalDays(), result.getIntervalDays(),
                  previousState.getCardState(), result.getCardState());
        
        return result;
    }

    /**
     * Get or create study state for a card-user combination
     */
    private StudyState getOrCreateStudyState(Long cardId, Long userId) {
        return studyStateRepository.findByUserIdAndCardId(userId, cardId)
                .orElseGet(() -> createNewStudyState(cardId, userId));
    }

    /**
     * Create a new study state for a card
     */
    private StudyState createNewStudyState(Long cardId, Long userId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + cardId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        StudyState studyState = new StudyState();
        studyState.setCard(card);
        studyState.setUser(user);
        studyState.setDeck(card.getDeck());
        studyState.setCardState(CardState.NEW);
        studyState.setEaseFactor(INITIAL_EASE_FACTOR);
        studyState.setRepetitionCount(0);
        studyState.setIntervalDays(1);
        studyState.setDueDate(LocalDateTime.now());
        studyState.setTotalReviews(0);
        studyState.setCorrectReviews(0);
        studyState.setAccuracyRate(0.0);
        studyState.setConsecutiveFailures(0);
        studyState.setCurrentLearningStep(0);
        studyState.setIsLeech(false);

        return studyState;
    }

    /**
     * Process learning phase cards (new cards and failed review cards)
     */
    private void processLearningCard(StudyState state, int grade, LocalDateTime reviewTime) {
        int currentStep = getCurrentLearningStep(state);
        
        switch (grade) {
            case 0: // Again - restart learning
                state.setCardState(CardState.LEARNING);
                currentStep = 0;
                setLearningInterval(state, currentStep, reviewTime);
                decreaseEaseFactor(state, 0.2);
                incrementConsecutiveFailures(state);
                break;
                
            case 1: // Hard - repeat current step  
                state.setCardState(CardState.LEARNING);
                setLearningInterval(state, currentStep, reviewTime);
                decreaseEaseFactor(state, 0.15);
                break;
                
            case 2: // Good - advance to next step
                state.setCardState(CardState.LEARNING);
                if (currentStep < LEARNING_STEPS.size() - 1) {
                    currentStep++;
                    setLearningInterval(state, currentStep, reviewTime);
                } else {
                    // Graduate to review
                    graduateCard(state, GRADUATION_INTERVAL, reviewTime);
                }
                resetConsecutiveFailures(state);
                break;
                
            case 3: // Easy - graduate immediately
                graduateCard(state, EASY_GRADUATION_INTERVAL, reviewTime);
                increaseEaseFactor(state, 0.15);
                resetConsecutiveFailures(state);
                break;
        }
        
        updateLearningStep(state, currentStep);
    }

    /**
     * Process review phase cards (graduated cards)
     */
    private void processReviewCard(StudyState state, int grade, LocalDateTime reviewTime) {
        double easeFactor = state.getEaseFactor();
        int currentInterval = state.getIntervalDays();
        
        switch (grade) {
            case 0: // Again - send back to learning
                state.setCardState(CardState.RELEARNING);
                setLearningInterval(state, 0, reviewTime);
                decreaseEaseFactor(state, 0.2);
                incrementConsecutiveFailures(state);
                updateLearningStep(state, 0);
                break;
                
            case 1: // Hard
                int hardInterval = Math.max(1, (int) (currentInterval * HARD_MULTIPLIER));
                state.setIntervalDays(hardInterval);
                state.setDueDate(reviewTime.plusDays(hardInterval));
                decreaseEaseFactor(state, 0.15);
                break;
                
            case 2: // Good  
                int goodInterval = Math.max(1, (int) (currentInterval * easeFactor));
                state.setIntervalDays(goodInterval);
                state.setDueDate(reviewTime.plusDays(goodInterval));
                state.setRepetitionCount(state.getRepetitionCount() + 1);
                resetConsecutiveFailures(state);
                break;
                
            case 3: // Easy
                int easyInterval = Math.max(1, (int) (currentInterval * easeFactor * EASY_MULTIPLIER));
                state.setIntervalDays(easyInterval);
                state.setDueDate(reviewTime.plusDays(easyInterval));
                state.setRepetitionCount(state.getRepetitionCount() + 1);
                increaseEaseFactor(state, 0.1);
                resetConsecutiveFailures(state);
                break;
        }
    }

    /**
     * Graduate a card from learning to review phase
     */
    private void graduateCard(StudyState state, double intervalDays, LocalDateTime reviewTime) {
        state.setCardState(CardState.REVIEW);
        state.setIntervalDays((int) intervalDays);
        state.setDueDate(reviewTime.plusDays((long) intervalDays));
        state.setRepetitionCount(1);
        state.setGraduatedAt(reviewTime);
        
        // Clear learning step data
        updateLearningStep(state, -1); // -1 indicates not in learning
    }

    /**
     * Set interval for learning phase (in minutes)
     */
    private void setLearningInterval(StudyState state, int stepIndex, LocalDateTime reviewTime) {
        int intervalMinutes = LEARNING_STEPS.get(stepIndex);
        state.setDueDate(reviewTime.plusMinutes(intervalMinutes));
        // Store the step for tracking learning progress
    }

    /**
     * Apply interval constraints and fuzz
     */
    private void applyIntervalConstraints(StudyState state) {
        if (state.getCardState() == CardState.REVIEW) {
            int interval = state.getIntervalDays();
            
            // Apply fuzz to prevent card synchronization (±5%)
            if (interval > 2) {
                double fuzzRange = interval * INTERVAL_FUZZ;
                double fuzzAmount = (Math.random() * 2 - 1) * fuzzRange;
                interval = Math.max(1, (int) (interval + fuzzAmount));
            }
            
            // Respect maximum interval
            interval = Math.min(interval, MAX_INTERVAL_DAYS);
            
            state.setIntervalDays(interval);
            // Recalculate due date with fuzzed interval
            if (state.getLastReviewDate() != null) {
                state.setDueDate(state.getLastReviewDate().plusDays(interval));
            }
        }
    }

    /**
     * Update review statistics
     */
    private void updateReviewStatistics(StudyState state, int grade, LocalDateTime reviewTime) {
        state.setLastReviewDate(reviewTime);
        state.setLastScore(grade);
        state.setTotalReviews(state.getTotalReviews() + 1);
        
        if (grade >= 2) { // Grade 2 (Good) and 3 (Easy) are correct
            state.setCorrectReviews(state.getCorrectReviews() + 1);
        }
        
        state.updateAccuracyRate();
        
        // Check for leech cards
        if (getConsecutiveFailures(state) >= LEECH_THRESHOLD) {
            markAsLeech(state);
        }
    }

    /**
     * Increase ease factor with bounds checking
     */
    private void increaseEaseFactor(StudyState state, double amount) {
        double newEF = Math.min(MAX_EASE_FACTOR, state.getEaseFactor() + amount);
        state.setEaseFactor(newEF);
    }

    /**
     * Decrease ease factor with bounds checking
     */
    private void decreaseEaseFactor(StudyState state, double amount) {
        double newEF = Math.max(MIN_EASE_FACTOR, state.getEaseFactor() - amount);
        state.setEaseFactor(newEF);
    }

    /**
     * Get due cards for a user with pagination
     */
    public Page<StudyState> getDueCards(Long userId, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        return studyStateRepository.findDueCardsByUser(userId, now, pageable);
    }

    /**
     * Get due cards for a user with pagination (DTO response)
     */
    public Page<StudyStateResponseDto> getDueCardsDto(Long userId, Pageable pageable) {
        Page<StudyState> studyStates = getDueCards(userId, pageable);
        return studyStates.map(studyStateMapper::toResponseDto);
    }

    /**
     * Get due cards count for a user
     */
    public long getDueCardsCount(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return studyStateRepository.countDueCardsByUser(userId, now);
    }

    /**
     * Get new cards for a user (includes cards without StudyState)
     */
    public Page<StudyState> getNewCards(Long userId, Pageable pageable) {
        // First, get existing NEW state cards
        Page<StudyState> existingNewCards = studyStateRepository.findNewCardsByUser(userId, pageable);
        
        // If we have space for more cards, get cards without StudyState
        if (!existingNewCards.hasContent() || existingNewCards.getContent().size() < pageable.getPageSize()) {
            Page<Card> cardsWithoutState = studyStateRepository.findCardsWithoutStudyState(userId, pageable);
            
            // Create temporary StudyState objects for these cards (not persisted yet)
            if (cardsWithoutState.hasContent()) {
                List<StudyState> tempStudyStates = cardsWithoutState.getContent().stream()
                        .map(card -> createTempStudyStateForCard(card, userId))
                        .toList();
                
                // Combine existing and temp states
                List<StudyState> allNewCards = new ArrayList<>(existingNewCards.getContent());
                allNewCards.addAll(tempStudyStates);
                
                return new PageImpl<>(allNewCards, pageable, 
                    existingNewCards.getTotalElements() + cardsWithoutState.getTotalElements());
            }
        }
        
        return existingNewCards;
    }

    /**
     * Create temporary StudyState for a card (not persisted)
     */
    private StudyState createTempStudyStateForCard(Card card, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        StudyState studyState = new StudyState();
        studyState.setCard(card);
        studyState.setUser(user);
        studyState.setDeck(card.getDeck());
        studyState.setCardState(CardState.NEW);
        studyState.setEaseFactor(INITIAL_EASE_FACTOR);
        studyState.setRepetitionCount(0);
        studyState.setIntervalDays(1);
        studyState.setDueDate(LocalDateTime.now()); // New cards are immediately available
        studyState.setTotalReviews(0);
        studyState.setCorrectReviews(0);
        studyState.setAccuracyRate(0.0);
        studyState.setConsecutiveFailures(0);
        studyState.setCurrentLearningStep(0);
        studyState.setIsLeech(false);

        return studyState;
    }

    /**
     * Get learning cards for a user
     */
    public List<StudyState> getLearningCards(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return studyStateRepository.findLearningCardsByUser(userId, now);
    }

    /**
     * Get next card for review (prioritizes due learning cards, then due review cards, then new cards)
     */
    public Optional<StudyState> getNextCard(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        
        // First priority: Learning cards that are due
        List<StudyState> learningCards = studyStateRepository.findLearningCardsByUser(userId, now);
        if (!learningCards.isEmpty()) {
            return Optional.of(learningCards.get(0));
        }
        
        // Second priority: Review cards that are due
        Page<StudyState> dueCards = studyStateRepository.findDueCardsByUser(userId, now, PageRequest.of(0, 1));
        if (dueCards.hasContent()) {
            return Optional.of(dueCards.getContent().get(0));
        }
        
        // Third priority: New cards (existing StudyState with NEW state)
        Page<StudyState> existingNewCards = studyStateRepository.findNewCardsByUser(userId, PageRequest.of(0, 1));
        if (existingNewCards.hasContent()) {
            return Optional.of(existingNewCards.getContent().get(0));
        }
        
        // Fourth priority: Cards without StudyState (truly new cards)
        Page<Card> cardsWithoutState = studyStateRepository.findCardsWithoutStudyState(userId, PageRequest.of(0, 1));
        if (cardsWithoutState.hasContent()) {
            Card card = cardsWithoutState.getContent().get(0);
            StudyState tempStudyState = createTempStudyStateForCard(card, userId);
            return Optional.of(tempStudyState);
        }
        
        return Optional.empty();
    }

    /**
     * Get the next available study time for a user
     * Returns null if user has cards currently due or new cards available
     */
    public LocalDateTime getNextAvailableStudyTime(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        
        // If user has cards currently available, they can study now
        if (hasCardsAvailableNow(userId)) {
            return null; // Can study immediately
        }
        
        // Find the earliest future due date
        return studyStateRepository.findNextDueTime(userId, now).orElse(null);
    }
    
    /**
     * Check if user has any cards available to study right now
     */
    public boolean hasCardsAvailableNow(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        
        // Check learning cards that are due
        List<StudyState> learningCards = studyStateRepository.findLearningCardsByUser(userId, now);
        if (!learningCards.isEmpty()) {
            return true;
        }
        
        // Check review cards that are due
        Long dueCardsCount = studyStateRepository.countDueCardsByUser(userId, now);
        if (dueCardsCount > 0) {
            return true;
        }
        
        // Check new cards (existing StudyState with NEW state)
        Page<StudyState> existingNewCards = studyStateRepository.findNewCardsByUser(userId, PageRequest.of(0, 1));
        if (existingNewCards.hasContent()) {
            return true;
        }
        
        // Check cards without StudyState (truly new cards)
        long cardsWithoutState = studyStateRepository.countCardsWithoutStudyState(userId);
        return cardsWithoutState > 0;
    }

    /**
     * Get detailed card counts for a user
     */
    public CardCounts getDetailedCardCounts(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        
        // Count learning cards (due now)
        List<StudyState> learningCards = studyStateRepository.findLearningCardsByUser(userId, now);
        int learningCount = learningCards.size();
        
        // Count review cards (due now, excluding learning cards)
        Long totalDue = studyStateRepository.countDueCardsByUser(userId, now);
        int reviewCount = Math.max(0, (int) (totalDue - learningCount));
        
        // Count new cards (existing StudyState with NEW state)
        Page<StudyState> existingNewCards = studyStateRepository.findNewCardsByUser(userId, PageRequest.of(0, Integer.MAX_VALUE));
        int existingNewCount = (int) existingNewCards.getTotalElements();
        
        // Count truly new cards (without StudyState)
        long cardsWithoutState = studyStateRepository.countCardsWithoutStudyState(userId);
        int totalNewCards = existingNewCount + (int) cardsWithoutState;
        
        return new CardCounts(totalNewCards, reviewCount, learningCount, (int) totalDue.longValue());
    }

    /**
     * Get detailed card counts for a specific deck
     */
    public CardCounts getDetailedCardCountsByDeck(Long userId, Long deckId) {
        LocalDateTime now = LocalDateTime.now();
        
        // Count learning cards for this deck
        Long learningCount = studyStateRepository.countLearningCardsByUserAndDeck(userId, deckId, now);
        
        // Count total due cards for this deck
        Long totalDue = studyStateRepository.countDueCardsByUserAndDeck(userId, deckId, now);
        
        // Count review cards (due cards excluding learning cards)
        int reviewCount = Math.max(0, (int) (totalDue - learningCount));
        
        // Count new cards for this deck
        Long existingNewCount = studyStateRepository.countNewCardsByUserAndDeck(userId, deckId);
        Long cardsWithoutState = studyStateRepository.countCardsWithoutStudyStateByDeck(userId, deckId);
        int totalNewCards = (int) (existingNewCount + cardsWithoutState);
        
        return new CardCounts(totalNewCards, reviewCount, learningCount.intValue(), totalDue.intValue());
    }

    /**
     * Get next available study time for a specific deck
     */
    public LocalDateTime getNextAvailableStudyTimeByDeck(Long userId, Long deckId) {
        LocalDateTime now = LocalDateTime.now();
        
        // If user has cards currently available in this deck, they can study now
        if (hasCardsAvailableNowByDeck(userId, deckId)) {
            return null; // Can study immediately
        }
        
        // Find the earliest future due date for this deck
        return studyStateRepository.findNextDueTimeByDeck(userId, deckId, now).orElse(null);
    }

    /**
     * Check if user has any cards available to study right now in a specific deck
     */
    public boolean hasCardsAvailableNowByDeck(Long userId, Long deckId) {
        LocalDateTime now = LocalDateTime.now();
        
        // Check if deck has learning cards due
        Long learningCount = studyStateRepository.countLearningCardsByUserAndDeck(userId, deckId, now);
        if (learningCount > 0) {
            return true;
        }
        
        // Check if deck has review cards due
        Long dueCount = studyStateRepository.countDueCardsByUserAndDeck(userId, deckId, now);
        if (dueCount > 0) {
            return true;
        }
        
        // Check if deck has new cards available
        Long newCardsCount = studyStateRepository.countNewCardsByUserAndDeck(userId, deckId);
        Long cardsWithoutState = studyStateRepository.countCardsWithoutStudyStateByDeck(userId, deckId);
        
        return (newCardsCount + cardsWithoutState) > 0;
    }

    /**
     * Calculate deck statistics for a user
     */
    public DeckStatistics calculateDeckStatistics(Long userId, Long deckId) {
        Object[] stats = studyStateRepository.calculateDeckStatistics(userId, deckId);
        
        if (stats != null && stats.length >= 6) {
            Long totalCards = (Long) stats[0];
            Long studiedCards = (Long) stats[1];
            Double avgAccuracy = (Double) stats[2];
            Long masteredCards = (Long) stats[3];
            Long totalReviews = (Long) stats[4];
            Long correctReviews = (Long) stats[5];
            
            double completionRate = totalCards > 0 ? (double) studiedCards / totalCards * 100.0 : 0.0;
            double averageAccuracy = avgAccuracy != null ? avgAccuracy : 0.0;
            
            return new DeckStatistics(
                    totalCards.intValue(),
                    studiedCards.intValue(),
                    masteredCards.intValue(),
                    averageAccuracy,
                    completionRate,
                    totalReviews.intValue(),
                    correctReviews.intValue()
            );
        }
        
        // Fallback if query returns no results
        Long totalCards = studyStateRepository.countCardsByDeck(deckId);
        return new DeckStatistics(
                totalCards.intValue(), 0, 0, 0.0, 0.0, 0, 0
        );
    }

    /**
     * Data class for deck statistics
     */
    public static class DeckStatistics {
        public final int totalCards;
        public final int studiedCards;
        public final int masteredCards;
        public final double averageAccuracy;
        public final double completionRate;
        public final int totalReviews;
        public final int correctReviews;
        
        public DeckStatistics(int totalCards, int studiedCards, int masteredCards, 
                            double averageAccuracy, double completionRate,
                            int totalReviews, int correctReviews) {
            this.totalCards = totalCards;
            this.studiedCards = studiedCards;
            this.masteredCards = masteredCards;
            this.averageAccuracy = averageAccuracy;
            this.completionRate = completionRate;
            this.totalReviews = totalReviews;
            this.correctReviews = correctReviews;
        }
    }

    /**
     * Data class for card counts
     */
    public static class CardCounts {
        public final int newCards;
        public final int reviewCards;
        public final int learningCards;
        public final int totalDue;
        
        public CardCounts(int newCards, int reviewCards, int learningCards, int totalDue) {
            this.newCards = newCards;
            this.reviewCards = reviewCards;
            this.learningCards = learningCards;
            this.totalDue = totalDue;
        }
    }

    /**
     * Calculate user statistics
     */
    public UserStatistics calculateUserStatistics(Long userId) {
        Object[] stats = studyStateRepository.calculateUserStatistics(userId);
        
        if (stats != null && stats.length >= 4) {
            Long totalCards = (Long) stats[0];
            Double avgAccuracy = (Double) stats[1];
            Long totalReviews = (Long) stats[2];
            Long correctReviews = (Long) stats[3];
            
            return new UserStatistics(
                totalCards != null ? totalCards : 0L,
                avgAccuracy != null ? avgAccuracy : 0.0,
                totalReviews != null ? totalReviews : 0L,
                correctReviews != null ? correctReviews : 0L,
                getDueCardsCount(userId)
            );
        }
        
        return new UserStatistics(0L, 0.0, 0L, 0L, 0L);
    }

    // Helper methods for learning step tracking
    private int getCurrentLearningStep(StudyState state) {
        return state.getCurrentLearningStep() != null ? state.getCurrentLearningStep() : 0;
    }

    private void updateLearningStep(StudyState state, int step) {
        state.setCurrentLearningStep(step);
    }

    private int getConsecutiveFailures(StudyState state) {
        return state.getConsecutiveFailures() != null ? state.getConsecutiveFailures() : 0;
    }

    private void incrementConsecutiveFailures(StudyState state) {
        int current = getConsecutiveFailures(state);
        state.setConsecutiveFailures(current + 1);
    }

    private void resetConsecutiveFailures(StudyState state) {
        state.setConsecutiveFailures(0);
    }

    private void markAsLeech(StudyState state) {
        state.setIsLeech(true);
        log.warn("Card {} marked as leech for user {} after {} consecutive failures", 
                state.getCard().getId(), state.getUser().getId(), getConsecutiveFailures(state));
        // Could implement additional leech handling logic here (suspend, reset difficulty, etc.)
    }

    private void validateGrade(int grade) {
        if (grade < 0 || grade > 3) {
            throw new IllegalArgumentException("Grade must be between 0 and 3, got: " + grade);
        }
    }

    private StudyState cloneState(StudyState state) {
        // Simple clone for logging purposes
        StudyState clone = new StudyState();
        clone.setIntervalDays(state.getIntervalDays());
        clone.setCardState(state.getCardState());
        return clone;
    }

    /**
     * Data class for user statistics
     */
    public static class UserStatistics {
        private final Long totalCards;
        private final Double averageAccuracy;
        private final Long totalReviews;
        private final Long correctReviews;
        private final Long dueCards;

        public UserStatistics(Long totalCards, Double averageAccuracy, Long totalReviews, 
                            Long correctReviews, Long dueCards) {
            this.totalCards = totalCards;
            this.averageAccuracy = averageAccuracy;
            this.totalReviews = totalReviews;
            this.correctReviews = correctReviews;
            this.dueCards = dueCards;
        }

        // Getters
        public Long getTotalCards() { return totalCards; }
        public Double getAverageAccuracy() { return averageAccuracy; }
        public Long getTotalReviews() { return totalReviews; }
        public Long getCorrectReviews() { return correctReviews; }
        public Long getDueCards() { return dueCards; }
    }
}
