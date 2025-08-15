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
        
        ReviewResultDto result = new ReviewResultDto();
        result.setSuccess(true);
        result.setMessage(getReviewMessage(reviewDto.getGrade()));
        result.setUpdatedStudyState(practiceMapper.toSimplifiedStudyStateDto(updatedState));
        result.setNextCard(nextCard);
        result.setSessionProgress(progress);
        
        log.debug("Processed review for card {} with grade {} for user {}", 
                cardId, reviewDto.getGrade(), userId);
        
        return result;
    }

    /**
     * Get count of due cards
     */
    @Transactional(readOnly = true)
    public DueCardsCountDto getDueCardsCount(Long userId, Long deckId) {
        long totalDue = studyStateService.getDueCardsCount(userId);
        
        // TODO: Implement deck-specific counting if deckId is provided
        
        return DueCardsCountDto.builder()
                .totalDue((int) totalDue)
                .newCards(0) // TODO: Implement
                .reviewCards(0) // TODO: Implement
                .learningCards(0) // TODO: Implement
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
        // TODO: Implement deck-specific statistics
        
        return DeckStatisticsDto.builder()
                .deckId(deckId)
                .totalCards(0)
                .studiedCards(0)
                .masteredCards(0)
                .averageAccuracy(0.0)
                .completionRate(0.0)
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
}
