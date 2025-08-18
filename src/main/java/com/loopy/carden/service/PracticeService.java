package com.loopy.carden.service;

import com.loopy.carden.dto.practice.*;
import com.loopy.carden.entity.Card;
import com.loopy.carden.entity.ReviewSession;
import com.loopy.carden.entity.StudyState;
import com.loopy.carden.exception.ResourceNotFoundException;
import com.loopy.carden.mapper.PracticeMapper;
import com.loopy.carden.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service for practice functionality and SRS operations
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PracticeService {

    private final StudyStateService studyStateService;
    private final ReviewSessionService reviewSessionService;
    private final CardRepository cardRepository;
    private final PracticeMapper practiceMapper;
    private final AnswerValidationService answerValidationService;
    private final DistractorGenerationService distractorGenerationService;

    /**
     * Start a new practice session
     */
    public PracticeSessionDto startPracticeSession(Long userId, PracticeSessionStartDto startDto) {
        // Create new review session
        ReviewSession session = reviewSessionService.startSession(userId, startDto.getDeckId(), startDto.getStudyMode());
        
        // Get initial card counts
        long dueCount = studyStateService.getDueCardsCount(userId);
        
        log.info("Started practice session {} for user {} with {} due cards", 
                session.getId(), userId, dueCount);
        
        return PracticeSessionDto.builder()
                .sessionId(session.getId())
                .studyMode(session.getStudyMode())
                .deckId(session.getDeck() != null ? session.getDeck().getId() : null)
                .startTime(session.getSessionDate())
                .dueCardsCount((int) dueCount)
                .maxNewCards(startDto.getMaxNewCards())
                .maxReviewCards(startDto.getMaxReviewCards())
                .includeNewCards(startDto.getIncludeNewCards())
                .includeReviewCards(startDto.getIncludeReviewCards())
                .includeLearningCards(startDto.getIncludeLearningCards())
                .build();
    }

    /**
     * Get current active practice session
     */
    @Transactional(readOnly = true)
    public PracticeSessionDto getCurrentSession(Long userId) {
        Optional<ReviewSession> activeSession = reviewSessionService.getActiveSession(userId);
        
        if (activeSession.isEmpty()) {
            throw new ResourceNotFoundException("No active practice session found for user: " + userId);
        }
        
        ReviewSession session = activeSession.get();
        
        return PracticeSessionDto.builder()
                .sessionId(session.getId())
                .studyMode(session.getStudyMode())
                .deckId(session.getDeck() != null ? session.getDeck().getId() : null)
                .startTime(session.getSessionDate())
                .cardsStudied(session.getCardsStudied())
                .cardsCorrect(session.getCardsCorrect())
                .currentAccuracy(session.getAccuracyRate())
                .durationMinutes(session.getDurationMinutes())
                .build();
    }

    /**
     * Complete practice session
     */
    public SessionSummaryDto completeSession(Long sessionId, Long userId) {
        ReviewSession completedSession = reviewSessionService.completeSession(sessionId, userId);
        
        // Calculate next study time information
        LocalDateTime nextStudyTime = studyStateService.getNextAvailableStudyTime(userId);
        boolean canStudyNow = studyStateService.hasCardsAvailableNow(userId);
        Long minutesUntilNext = null;
        
        if (nextStudyTime != null && !canStudyNow) {
            LocalDateTime now = LocalDateTime.now();
            minutesUntilNext = java.time.Duration.between(now, nextStudyTime).toMinutes();
        }
        
        return SessionSummaryDto.builder()
                .sessionId(completedSession.getId())
                .totalCards(completedSession.getCardsStudied())
                .correctCards(completedSession.getCardsCorrect())
                .finalAccuracy(completedSession.getAccuracyRate())
                .durationMinutes(completedSession.getDurationMinutes())
                .newCards(completedSession.getNewCards())
                .reviewCards(completedSession.getReviewCards())
                .relearningCards(completedSession.getRelearningCards())
                .sessionStats(completedSession.getSessionStats())
                .completedAt(LocalDateTime.now())
                .nextAvailableStudyTime(nextStudyTime)
                .minutesUntilNextCard(minutesUntilNext)
                .canStudyNow(canStudyNow)
                .build();
    }

    /**
     * Get due cards for practice
     */
    @Transactional(readOnly = true)
    public Page<PracticeCardDto> getDueCards(Long userId, Long deckId, Pageable pageable) {
        Page<StudyState> dueStates = studyStateService.getDueCards(userId, pageable);
        
        return dueStates.map(state -> practiceMapper.toPracticeCardDto(state, deckId));
    }

    /**
     * Get next card to practice with intelligent prioritization
     */
    @Transactional(readOnly = true)
    public PracticeCardDto getNextCard(Long userId, Long deckId) {
        Optional<StudyState> nextState = studyStateService.getNextCard(userId);
        
        if (nextState.isEmpty()) {
            throw new ResourceNotFoundException("No cards available for practice");
        }
        
        return practiceMapper.toPracticeCardDto(nextState.get(), deckId);
    }

    /**
     * Submit a card review and update SRS state
     */
    public ReviewResultDto submitReview(Long cardId, Long userId, ReviewSubmissionDto reviewDto) {
        // Process the review through SRS algorithm
        StudyState updatedState = studyStateService.processReview(
                cardId, userId, reviewDto.getGrade(), LocalDateTime.now());
        
        // Update active session if exists
        Optional<ReviewSession> activeSession = reviewSessionService.getActiveSession(userId);
        if (activeSession.isPresent()) {
            boolean isNewCard = updatedState.getTotalReviews() == 1;
            reviewSessionService.recordCardReview(
                    activeSession.get().getId(), 
                    userId, 
                    reviewDto.getGrade(), 
                    reviewDto.getResponseTimeMs(),
                    isNewCard
            );
        }
        
        // Get next card
        Optional<StudyState> nextCardState = studyStateService.getNextCard(userId);
        PracticeCardDto nextCard = nextCardState.map(state -> practiceMapper.toPracticeCardDto(state, null))
                .orElse(null);
        
        // Build session progress
        ReviewResultDto.SessionProgressDto progress = null;
        if (activeSession.isPresent()) {
            ReviewSession session = activeSession.get();
            progress = new ReviewResultDto.SessionProgressDto();
            progress.setCardsStudied(session.getCardsStudied());
            progress.setCardsCorrect(session.getCardsCorrect());
            progress.setCurrentAccuracy(session.getAccuracyRate());
            progress.setRemainingCards((int) studyStateService.getDueCardsCount(userId));
            progress.setSessionDurationMinutes(session.getDurationMinutes());
        }
        
        ReviewResultDto result = ReviewResultDto.builder()
                .cardId(cardId)
                .grade(reviewDto.getGrade())
                .isCorrect(reviewDto.getGrade() >= 2)
                .feedback(getReviewMessage(reviewDto.getGrade()))
                .message(getReviewMessage(reviewDto.getGrade()))
                .success(true)
                .updatedStudyState(practiceMapper.toSimplifiedStudyStateDto(updatedState))
                .nextCard(nextCard)
                .sessionProgress(progress)
                .build();
        
        log.debug("Processed review for card {} with grade {} for user {}", 
                cardId, reviewDto.getGrade(), userId);
        
        return result;
    }

    /**
     * Get count of due cards
     */
    @Transactional(readOnly = true)
    public DueCardsCountDto getDueCardsCount(Long userId, Long deckId) {
        // Get detailed card counts (deck-specific or global)
        StudyStateService.CardCounts cardCounts;
        LocalDateTime nextStudyTime;
        boolean hasCardsAvailable;
        
        if (deckId != null) {
            // Deck-specific counting
            cardCounts = studyStateService.getDetailedCardCountsByDeck(userId, deckId);
            nextStudyTime = studyStateService.getNextAvailableStudyTimeByDeck(userId, deckId);
            hasCardsAvailable = studyStateService.hasCardsAvailableNowByDeck(userId, deckId);
        } else {
            // Global counting
            cardCounts = studyStateService.getDetailedCardCounts(userId);
            nextStudyTime = studyStateService.getNextAvailableStudyTime(userId);
            hasCardsAvailable = studyStateService.hasCardsAvailableNow(userId);
        }
        
        // Calculate minutes until next card is available
        Long minutesUntilNext = null;
        if (nextStudyTime != null && !hasCardsAvailable) {
            LocalDateTime now = LocalDateTime.now();
            minutesUntilNext = java.time.Duration.between(now, nextStudyTime).toMinutes();
        }
        
        return DueCardsCountDto.builder()
                .totalDue(cardCounts.totalDue)
                .newCards(cardCounts.newCards)
                .reviewCards(cardCounts.reviewCards)
                .learningCards(cardCounts.learningCards)
                .nextCardAvailableAt(nextStudyTime)
                .minutesUntilNext(minutesUntilNext)
                .hasCardsAvailable(hasCardsAvailable)
                .build();
    }

    /**
     * Get new cards for study
     */
    @Transactional(readOnly = true)
    public Page<PracticeCardDto> getNewCards(Long userId, Long deckId, Pageable pageable) {
        Page<StudyState> newStates = studyStateService.getNewCards(userId, pageable);
        
        return newStates.map(state -> practiceMapper.toPracticeCardDto(state, deckId));
    }

    /**
     * Get learning cards
     */
    @Transactional(readOnly = true)
    public List<PracticeCardDto> getLearningCards(Long userId, Long deckId) {
        List<StudyState> learningStates = studyStateService.getLearningCards(userId);
        
        return learningStates.stream()
                .map(state -> practiceMapper.toPracticeCardDto(state, deckId))
                .toList();
    }

    /**
     * Get deck-specific statistics
     */
    @Transactional(readOnly = true)
    public DeckStatisticsDto getDeckStatistics(Long userId, Long deckId) {
        StudyStateService.DeckStatistics stats = studyStateService.calculateDeckStatistics(userId, deckId);
        
        return DeckStatisticsDto.builder()
                .deckId(deckId)
                .totalCards(stats.totalCards)
                .studiedCards(stats.studiedCards)
                .masteredCards(stats.masteredCards)
                .averageAccuracy(stats.averageAccuracy)
                .completionRate(stats.completionRate)
                .build();
    }

    /**
     * Get card for type-answer mode
     */
    @Transactional(readOnly = true)
    public TypeAnswerCardDto getTypeAnswerCard(Long cardId, Long userId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardId));
        
        TypeAnswerCardDto dto = new TypeAnswerCardDto();
        // Copy base properties from PracticeCardDto
        practiceMapper.copyBasePracticeCardProperties(card, dto);
        
        // Set type-answer specific properties
        dto.setPlaceholder("Type your answer here...");
        dto.setMaxLength(card.getBack().length() + 20); // Allow some extra length
        dto.setCaseSensitive(false);
        dto.setShowHint(true);
        
        return dto;
    }

    /**
     * Get card for multiple choice mode
     */
    @Transactional(readOnly = true)
    public MultipleChoiceCardDto getMultipleChoiceCard(Long cardId, Long userId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardId));
        
        MultipleChoiceCardDto dto = new MultipleChoiceCardDto();
        // Copy base properties from PracticeCardDto
        practiceMapper.copyBasePracticeCardProperties(card, dto);
        
        // Generate distractors
        List<String> distractors = distractorGenerationService.generateDistractors(card, 3);
        
        // Create options (1 correct + 3 distractors)
        List<MultipleChoiceCardDto.ChoiceOption> options = new ArrayList<>();
        options.add(new MultipleChoiceCardDto.ChoiceOption(card.getBack(), true));
        
        for (String distractor : distractors) {
            options.add(new MultipleChoiceCardDto.ChoiceOption(distractor, false));
        }
        
        // Shuffle options
        Collections.shuffle(options);
        
        // Find correct option index after shuffling
        int correctIndex = 0;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).isCorrect()) {
                correctIndex = i;
                break;
            }
        }
        
        dto.setOptions(options);
        dto.setCorrectOptionIndex(correctIndex);
        dto.setShuffled(true);
        
        return dto;
    }

    /**
     * Submit type-answer review
     */
    public ReviewResultDto submitTypeAnswerReview(Long userId, TypeAnswerSubmissionDto submission) {
        Card card = cardRepository.findById(submission.getCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + submission.getCardId()));
        
        // Validate answer using fuzzy matching
        AnswerValidationService.AnswerValidationResult validation = 
                answerValidationService.validateAnswer(submission.getUserAnswer(), card.getBack());
        
        int grade = validation.getGrade();
        
        // Process the review with SRS
        StudyState updatedState = studyStateService.processReview(
                submission.getCardId(), userId, grade, LocalDateTime.now());
        
        // Update session statistics
        reviewSessionService.updateSessionProgress(userId, 1, validation.isCorrect() ? 1 : 0);
        
        log.info("Type-answer review processed for user {} card {} with grade {} (similarity: {:.2f})", 
                userId, submission.getCardId(), grade, validation.getSimilarity());
        
        return ReviewResultDto.builder()
                .cardId(submission.getCardId())
                .grade(grade)
                .isCorrect(validation.isCorrect())
                .feedback(validation.getFeedback())
                .similarity(validation.getSimilarity())
                .correctAnswer(card.getBack())
                .userAnswer(submission.getUserAnswer())
                .nextReviewDate(updatedState.getDueDate())
                .currentState(updatedState.getCardState().name())
                .message(getTypeAnswerMessage(validation))
                .build();
    }

    /**
     * Submit multiple choice review
     */
    public ReviewResultDto submitMultipleChoiceReview(Long userId, MultipleChoiceSubmissionDto submission) {
        Card card = cardRepository.findById(submission.getCardId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + submission.getCardId()));
        
        // Get the multiple choice card to check correct answer
        MultipleChoiceCardDto mcCard = getMultipleChoiceCard(submission.getCardId(), userId);
        
        boolean isCorrect = submission.getSelectedOption().equals(mcCard.getCorrectOptionIndex());
        int grade = isCorrect ? 2 : 0; // Good for correct, Again for incorrect in MC
        
        // Process the review with SRS
        StudyState updatedState = studyStateService.processReview(
                submission.getCardId(), userId, grade, LocalDateTime.now());
        
        // Update session statistics
        reviewSessionService.updateSessionProgress(userId, 1, isCorrect ? 1 : 0);
        
        log.info("Multiple choice review processed for user {} card {} with grade {} (option: {})", 
                userId, submission.getCardId(), grade, submission.getSelectedOption());
        
        String selectedText = mcCard.getOptions().get(submission.getSelectedOption()).getText();
        
        return ReviewResultDto.builder()
                .cardId(submission.getCardId())
                .grade(grade)
                .isCorrect(isCorrect)
                .feedback(isCorrect ? "Correct!" : "Incorrect. The right answer was: " + card.getBack())
                .correctAnswer(card.getBack())
                .userAnswer(selectedText)
                .nextReviewDate(updatedState.getDueDate())
                .currentState(updatedState.getCardState().name())
                .message(getMultipleChoiceMessage(isCorrect))
                .build();
    }

    // Helper methods
    private String getReviewMessage(int grade) {
        return switch (grade) {
            case 0 -> "Don't worry! This card will be reviewed again soon.";
            case 1 -> "Getting better! Keep practicing this card.";
            case 2 -> "Good job! This card will be reviewed in a few days.";
            case 3 -> "Excellent! This card has been scheduled for later review.";
            default -> "Review processed successfully.";
        };
    }

    private String getTypeAnswerMessage(AnswerValidationService.AnswerValidationResult validation) {
        if (validation.isCorrect()) {
            if (validation.getSimilarity() >= 0.95) {
                return "Perfect! Your answer was spot on.";
            } else if (validation.getSimilarity() >= 0.90) {
                return "Great! Your answer was very close.";
            } else {
                return "Good! Your answer was close enough to be accepted.";
            }
        } else {
            if (validation.getSimilarity() > 0.7) {
                return "Very close! Check your spelling and try again.";
            } else if (validation.getSimilarity() > 0.4) {
                return "You're on the right track, but not quite there yet.";
            } else {
                return "Let's review this card again. Take your time!";
            }
        }
    }

    private String getMultipleChoiceMessage(boolean isCorrect) {
        return isCorrect ? 
                "Correct! Well done." : 
                "Incorrect. Review the correct answer and try to remember it for next time.";
    }
}
