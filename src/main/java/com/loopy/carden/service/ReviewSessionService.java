package com.loopy.carden.service;

import com.loopy.carden.dto.session.ReviewSessionResponseDto;
import com.loopy.carden.entity.Deck;
import com.loopy.carden.entity.ReviewSession;
import com.loopy.carden.entity.ReviewSession.SessionStatus;
import com.loopy.carden.entity.ReviewSession.SessionStats;
import com.loopy.carden.entity.ReviewSession.StudyMode;
import com.loopy.carden.entity.User;
import com.loopy.carden.exception.ResourceNotFoundException;
import com.loopy.carden.mapper.ReviewSessionMapper;
import com.loopy.carden.repository.DeckRepository;
import com.loopy.carden.repository.ReviewSessionRepository;
import com.loopy.carden.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing review sessions and tracking study progress
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReviewSessionService {

    private final ReviewSessionRepository reviewSessionRepository;
    private final UserRepository userRepository;
    private final DeckRepository deckRepository;
    private final ReviewSessionMapper reviewSessionMapper;

    /**
     * Start a new review session
     */
    public ReviewSession startSession(Long userId, Long deckId, StudyMode studyMode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Deck deck = null;
        if (deckId != null) {
            deck = deckRepository.findById(deckId)
                    .orElseThrow(() -> new ResourceNotFoundException("Deck not found with id: " + deckId));
        }

        // Check if there's an active session for this user
        Optional<ReviewSession> activeSession = reviewSessionRepository.findActiveSessionByUser(userId);
        if (activeSession.isPresent()) {
            log.warn("User {} already has an active session {}, completing it first", 
                    userId, activeSession.get().getId());
            completeSession(activeSession.get().getId(), userId);
        }

        ReviewSession session = new ReviewSession();
        session.setUser(user);
        session.setDeck(deck);
        session.setSessionDate(LocalDateTime.now());
        session.setStudyMode(studyMode);
        session.setSessionStatus(SessionStatus.IN_PROGRESS);
        
        // Initialize session stats
        SessionStats stats = new SessionStats();
        stats.setScoreDistribution(Arrays.asList(0, 0, 0, 0)); // [again, hard, good, easy]
        stats.setPauseCount(0);
        stats.setTotalTimeSpent(0);
        session.setSessionStats(stats);

        ReviewSession savedSession = reviewSessionRepository.save(session);
        
        log.info("Started new review session {} for user {} in deck {} with mode {}", 
                savedSession.getId(), userId, deckId, studyMode);
        
        return savedSession;
    }

    /**
     * Record a card review within a session
     */
    public ReviewSession recordCardReview(Long sessionId, Long userId, int grade, 
                                        int responseTimeMs, boolean isNewCard) {
        ReviewSession session = getActiveSession(sessionId, userId);
        
        // Update card counts
        session.setCardsStudied(session.getCardsStudied() + 1);
        
        if (grade >= 2) { // Grade 2 (Good) and 3 (Easy) are correct
            session.setCardsCorrect(session.getCardsCorrect() + 1);
        }
        
        // Update card type counts
        if (isNewCard) {
            session.setNewCards(session.getNewCards() + 1);
        } else if (grade == 0) {
            session.setRelearningCards(session.getRelearningCards() + 1);
        } else {
            session.setReviewCards(session.getReviewCards() + 1);
        }
        
        // Update session statistics
        updateSessionStats(session, grade, responseTimeMs);
        
        // Recalculate accuracy rate
        session.updateAccuracyRate();
        
        ReviewSession updatedSession = reviewSessionRepository.save(session);
        
        log.debug("Recorded review for session {}: grade={}, responseTime={}ms, newCard={}", 
                sessionId, grade, responseTimeMs, isNewCard);
        
        return updatedSession;
    }

    /**
     * Update session timing (when user pauses/resumes)
     */
    public ReviewSession updateSessionTiming(Long sessionId, Long userId, boolean isPause) {
        ReviewSession session = getActiveSession(sessionId, userId);
        SessionStats stats = session.getSessionStats();
        
        if (isPause) {
            stats.setPauseCount(stats.getPauseCount() + 1);
            log.debug("Session {} paused (pause count: {})", sessionId, stats.getPauseCount());
        }
        
        // Calculate duration so far
        long durationMinutes = ChronoUnit.MINUTES.between(session.getSessionDate(), LocalDateTime.now());
        session.setDurationMinutes((int) durationMinutes);
        
        return reviewSessionRepository.save(session);
    }

    /**
     * Update session progress (for type-answer and multiple-choice modes)
     */
    public ReviewSession updateSessionProgress(Long userId, int cardsStudied, int correctCards) {
        Optional<ReviewSession> activeSessionOpt = getActiveSession(userId);
        if (activeSessionOpt.isEmpty()) {
            log.debug("No active session found for user {} to update progress", userId);
            return null;
        }
        
        ReviewSession session = activeSessionOpt.get();
        
        // Update progress incrementally
        session.setCardsStudied(session.getCardsStudied() + cardsStudied);
        session.setCardsCorrect(session.getCardsCorrect() + correctCards);
        
        // Recalculate accuracy rate
        session.updateAccuracyRate();
        
        // Update duration
        long durationMinutes = ChronoUnit.MINUTES.between(session.getSessionDate(), LocalDateTime.now());
        session.setDurationMinutes((int) durationMinutes);
        
        ReviewSession updatedSession = reviewSessionRepository.save(session);
        
        log.debug("Updated session {} progress: +{} cards, +{} correct", 
                session.getId(), cardsStudied, correctCards);
        
        return updatedSession;
    }

    /**
     * Complete an active session
     */
    public ReviewSession completeSession(Long sessionId, Long userId) {
        ReviewSession session = getActiveSession(sessionId, userId);
        
        session.setSessionStatus(SessionStatus.COMPLETED);
        
        // Calculate final duration
        long durationMinutes = ChronoUnit.MINUTES.between(session.getSessionDate(), LocalDateTime.now());
        session.setDurationMinutes((int) durationMinutes);
        
        // Update final stats
        SessionStats stats = session.getSessionStats();
        if (stats != null) {
            stats.setTotalTimeSpent((int) ChronoUnit.SECONDS.between(session.getSessionDate(), LocalDateTime.now()));
        }
        
        ReviewSession completedSession = reviewSessionRepository.save(session);
        
        log.info("Completed session {} for user {}. Duration: {} minutes, Cards studied: {}, Accuracy: {}%", 
                sessionId, userId, completedSession.getDurationMinutes(), 
                completedSession.getCardsStudied(), completedSession.getAccuracyRate());
        
        return completedSession;
    }

    /**
     * Abandon an active session
     */
    public ReviewSession abandonSession(Long sessionId, Long userId) {
        ReviewSession session = getActiveSession(sessionId, userId);
        
        session.setSessionStatus(SessionStatus.ABANDONED);
        
        // Calculate duration up to abandonment
        long durationMinutes = ChronoUnit.MINUTES.between(session.getSessionDate(), LocalDateTime.now());
        session.setDurationMinutes((int) durationMinutes);
        
        ReviewSession abandonedSession = reviewSessionRepository.save(session);
        
        log.info("Abandoned session {} for user {} after {} minutes", 
                sessionId, userId, abandonedSession.getDurationMinutes());
        
        return abandonedSession;
    }

    /**
     * Get active session for a user
     */
    public Optional<ReviewSession> getActiveSession(Long userId) {
        return reviewSessionRepository.findActiveSessionByUser(userId);
    }

    /**
     * Get session by ID with user validation
     */
    public ReviewSession getSession(Long sessionId, Long userId) {
        ReviewSession session = reviewSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));
        
        if (!session.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Session does not belong to user: " + userId);
        }
        
        return session;
    }

    /**
     * Get user's session history
     */
    public Page<ReviewSession> getUserSessions(Long userId, Pageable pageable) {
        return reviewSessionRepository.findByUserIdOrderBySessionDateDesc(userId, pageable);
    }

    /**
     * Get user's session history (DTO response)
     */
    public Page<ReviewSessionResponseDto> getUserSessionsDto(Long userId, Pageable pageable) {
        Page<ReviewSession> sessions = getUserSessions(userId, pageable);
        return sessions.map(reviewSessionMapper::toResponseDto);
    }

    /**
     * Get user's sessions for a specific deck
     */
    public Page<ReviewSession> getUserSessionsForDeck(Long userId, Long deckId, Pageable pageable) {
        return reviewSessionRepository.findByUserIdAndDeckIdOrderBySessionDateDesc(userId, deckId, pageable);
    }

    /**
     * Get user's sessions within date range
     */
    public List<ReviewSession> getUserSessionsInDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return reviewSessionRepository.findByUserIdAndSessionDateBetween(userId, startDate, endDate);
    }

    /**
     * Calculate daily study statistics
     */
    public DailyStudyStats calculateDailyStats(Long userId, LocalDateTime date) {
        LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        List<ReviewSession> dailySessions = getUserSessionsInDateRange(userId, startOfDay, endOfDay);
        
        if (dailySessions.isEmpty()) {
            return new DailyStudyStats(0, 0, 0, 0.0, 0);
        }
        
        int totalSessions = dailySessions.size();
        int totalCards = dailySessions.stream().mapToInt(ReviewSession::getCardsStudied).sum();
        int totalMinutes = dailySessions.stream().mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0).sum();
        double avgAccuracy = dailySessions.stream()
                .mapToDouble(s -> s.getAccuracyRate() != null ? s.getAccuracyRate() : 0.0)
                .average()
                .orElse(0.0);
        int newCards = dailySessions.stream().mapToInt(ReviewSession::getNewCards).sum();
        
        return new DailyStudyStats(totalSessions, totalCards, totalMinutes, avgAccuracy, newCards);
    }

    /**
     * Get weekly study statistics
     */
    public WeeklyStudyStats calculateWeeklyStats(Long userId, LocalDateTime weekStart) {
        LocalDateTime weekEnd = weekStart.plusWeeks(1);
        List<ReviewSession> weeklySessions = getUserSessionsInDateRange(userId, weekStart, weekEnd);
        
        if (weeklySessions.isEmpty()) {
            return new WeeklyStudyStats(0, 0, 0, 0.0, 0, 0);
        }
        
        int totalSessions = weeklySessions.size();
        int totalCards = weeklySessions.stream().mapToInt(ReviewSession::getCardsStudied).sum();
        int totalMinutes = weeklySessions.stream().mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0).sum();
        double avgAccuracy = weeklySessions.stream()
                .mapToDouble(s -> s.getAccuracyRate() != null ? s.getAccuracyRate() : 0.0)
                .average()
                .orElse(0.0);
        int newCards = weeklySessions.stream().mapToInt(ReviewSession::getNewCards).sum();
        int studyDays = (int) weeklySessions.stream()
                .map(s -> s.getSessionDate().toLocalDate())
                .distinct()
                .count();
        
        return new WeeklyStudyStats(totalSessions, totalCards, totalMinutes, avgAccuracy, newCards, studyDays);
    }

    /**
     * Auto-complete abandoned sessions (maintenance task)
     */
    @Transactional
    public int autoCompleteAbandonedSessions(int hoursThreshold) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hoursThreshold);
        List<ReviewSession> abandonedSessions = reviewSessionRepository.findStaleActiveSessions(cutoffTime);
        
        for (ReviewSession session : abandonedSessions) {
            session.setSessionStatus(SessionStatus.ABANDONED);
            long durationMinutes = ChronoUnit.MINUTES.between(session.getSessionDate(), cutoffTime);
            session.setDurationMinutes((int) durationMinutes);
        }
        
        if (!abandonedSessions.isEmpty()) {
            reviewSessionRepository.saveAll(abandonedSessions);
            log.info("Auto-completed {} abandoned sessions older than {} hours", 
                    abandonedSessions.size(), hoursThreshold);
        }
        
        return abandonedSessions.size();
    }

    // Helper methods
    private ReviewSession getActiveSession(Long sessionId, Long userId) {
        ReviewSession session = getSession(sessionId, userId);
        
        if (!session.isInProgress()) {
            throw new IllegalStateException("Session " + sessionId + " is not active");
        }
        
        return session;
    }

    private void updateSessionStats(ReviewSession session, int grade, int responseTimeMs) {
        SessionStats stats = session.getSessionStats();
        if (stats == null) {
            stats = new SessionStats();
            stats.setScoreDistribution(Arrays.asList(0, 0, 0, 0));
            session.setSessionStats(stats);
        }
        
        // Update score distribution [again, hard, good, easy]
        List<Integer> scoreDistribution = stats.getScoreDistribution();
        if (scoreDistribution.size() >= 4) {
            scoreDistribution.set(grade, scoreDistribution.get(grade) + 1);
        }
        
        // Update average response time
        Integer currentAvg = stats.getAverageResponseTime();
        int cardsStudied = session.getCardsStudied();
        
        if (currentAvg == null) {
            stats.setAverageResponseTime(responseTimeMs);
        } else {
            // Calculate running average
            int newAvg = ((currentAvg * (cardsStudied - 1)) + responseTimeMs) / cardsStudied;
            stats.setAverageResponseTime(newAvg);
        }
    }

    // Data classes for statistics
    public static class DailyStudyStats {
        private final int totalSessions;
        private final int totalCards;
        private final int totalMinutes;
        private final double averageAccuracy;
        private final int newCards;

        public DailyStudyStats(int totalSessions, int totalCards, int totalMinutes, 
                             double averageAccuracy, int newCards) {
            this.totalSessions = totalSessions;
            this.totalCards = totalCards;
            this.totalMinutes = totalMinutes;
            this.averageAccuracy = averageAccuracy;
            this.newCards = newCards;
        }

        // Getters
        public int getTotalSessions() { return totalSessions; }
        public int getTotalCards() { return totalCards; }
        public int getTotalMinutes() { return totalMinutes; }
        public double getAverageAccuracy() { return averageAccuracy; }
        public int getNewCards() { return newCards; }
    }

    public static class WeeklyStudyStats {
        private final int totalSessions;
        private final int totalCards;
        private final int totalMinutes;
        private final double averageAccuracy;
        private final int newCards;
        private final int studyDays;

        public WeeklyStudyStats(int totalSessions, int totalCards, int totalMinutes, 
                              double averageAccuracy, int newCards, int studyDays) {
            this.totalSessions = totalSessions;
            this.totalCards = totalCards;
            this.totalMinutes = totalMinutes;
            this.averageAccuracy = averageAccuracy;
            this.newCards = newCards;
            this.studyDays = studyDays;
        }

        // Getters
        public int getTotalSessions() { return totalSessions; }
        public int getTotalCards() { return totalCards; }
        public int getTotalMinutes() { return totalMinutes; }
        public double getAverageAccuracy() { return averageAccuracy; }
        public int getNewCards() { return newCards; }
        public int getStudyDays() { return studyDays; }
    }
}
