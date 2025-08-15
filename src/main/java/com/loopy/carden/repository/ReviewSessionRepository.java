package com.loopy.carden.repository;

import com.loopy.carden.entity.ReviewSession;
import com.loopy.carden.entity.ReviewSession.SessionStatus;
import com.loopy.carden.entity.ReviewSession.StudyMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewSessionRepository extends JpaRepository<ReviewSession, Long> {

    /**
     * Find active session for a user
     */
    @Query("SELECT rs FROM ReviewSession rs " +
           "WHERE rs.user.id = :userId " +
           "AND rs.sessionStatus = 'IN_PROGRESS' " +
           "ORDER BY rs.sessionDate DESC")
    Optional<ReviewSession> findActiveSessionByUser(@Param("userId") Long userId);

    /**
     * Find user's sessions ordered by date
     */
    @Query("SELECT rs FROM ReviewSession rs " +
           "WHERE rs.user.id = :userId " +
           "ORDER BY rs.sessionDate DESC")
    Page<ReviewSession> findByUserIdOrderBySessionDateDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find user's sessions for a specific deck
     */
    @Query("SELECT rs FROM ReviewSession rs " +
           "WHERE rs.user.id = :userId " +
           "AND rs.deck.id = :deckId " +
           "ORDER BY rs.sessionDate DESC")
    Page<ReviewSession> findByUserIdAndDeckIdOrderBySessionDateDesc(@Param("userId") Long userId,
                                                                   @Param("deckId") Long deckId,
                                                                   Pageable pageable);

    /**
     * Find sessions within date range
     */
    @Query("SELECT rs FROM ReviewSession rs " +
           "WHERE rs.user.id = :userId " +
           "AND rs.sessionDate >= :startDate " +
           "AND rs.sessionDate < :endDate " +
           "ORDER BY rs.sessionDate DESC")
    List<ReviewSession> findByUserIdAndSessionDateBetween(@Param("userId") Long userId,
                                                         @Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate);

    /**
     * Find sessions by status
     */
    @Query("SELECT rs FROM ReviewSession rs " +
           "WHERE rs.user.id = :userId " +
           "AND rs.sessionStatus = :status " +
           "ORDER BY rs.sessionDate DESC")
    List<ReviewSession> findByUserIdAndSessionStatus(@Param("userId") Long userId,
                                                    @Param("status") SessionStatus status);

    /**
     * Find sessions by study mode
     */
    @Query("SELECT rs FROM ReviewSession rs " +
           "WHERE rs.user.id = :userId " +
           "AND rs.studyMode = :studyMode " +
           "ORDER BY rs.sessionDate DESC")
    List<ReviewSession> findByUserIdAndStudyMode(@Param("userId") Long userId,
                                                @Param("studyMode") StudyMode studyMode);

    /**
     * Find stale active sessions (for cleanup)
     */
    @Query("SELECT rs FROM ReviewSession rs " +
           "WHERE rs.sessionStatus = 'IN_PROGRESS' " +
           "AND rs.sessionDate < :cutoffTime")
    List<ReviewSession> findStaleActiveSessions(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Count sessions for a user
     */
    @Query("SELECT COUNT(rs) FROM ReviewSession rs " +
           "WHERE rs.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    /**
     * Count completed sessions for a user
     */
    @Query("SELECT COUNT(rs) FROM ReviewSession rs " +
           "WHERE rs.user.id = :userId " +
           "AND rs.sessionStatus = 'COMPLETED'")
    long countCompletedSessionsByUserId(@Param("userId") Long userId);

    /**
     * Calculate total study time for a user (in minutes)
     */
    @Query("SELECT COALESCE(SUM(rs.durationMinutes), 0) FROM ReviewSession rs " +
           "WHERE rs.user.id = :userId " +
           "AND rs.sessionStatus = 'COMPLETED'")
    long calculateTotalStudyTimeByUserId(@Param("userId") Long userId);

    /**
     * Calculate total cards studied by a user
     */
    @Query("SELECT COALESCE(SUM(rs.cardsStudied), 0) FROM ReviewSession rs " +
           "WHERE rs.user.id = :userId")
    long calculateTotalCardsStudiedByUserId(@Param("userId") Long userId);

    /**
     * Calculate average accuracy for a user
     */
    @Query("SELECT AVG(rs.accuracyRate) FROM ReviewSession rs " +
           "WHERE rs.user.id = :userId " +
           "AND rs.cardsStudied > 0")
    Double calculateAverageAccuracyByUserId(@Param("userId") Long userId);

    /**
     * Find recent sessions (last N days)
     */
    @Query("SELECT rs FROM ReviewSession rs " +
           "WHERE rs.user.id = :userId " +
           "AND rs.sessionDate >= :sinceDate " +
           "ORDER BY rs.sessionDate DESC")
    List<ReviewSession> findRecentSessionsByUser(@Param("userId") Long userId,
                                                @Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Find sessions with minimum study time
     */
    @Query("SELECT rs FROM ReviewSession rs " +
           "WHERE rs.user.id = :userId " +
           "AND rs.durationMinutes >= :minMinutes " +
           "ORDER BY rs.sessionDate DESC")
    List<ReviewSession> findSessionsWithMinStudyTime(@Param("userId") Long userId,
                                                    @Param("minMinutes") int minMinutes);

    /**
     * Find best sessions (highest accuracy)
     */
    @Query("SELECT rs FROM ReviewSession rs " +
           "WHERE rs.user.id = :userId " +
           "AND rs.cardsStudied >= :minCards " +
           "ORDER BY rs.accuracyRate DESC, rs.sessionDate DESC")
    List<ReviewSession> findBestSessionsByUser(@Param("userId") Long userId,
                                             @Param("minCards") int minCards,
                                             Pageable pageable);

    /**
     * Find sessions for a deck within date range
     */
    @Query("SELECT rs FROM ReviewSession rs " +
           "WHERE rs.deck.id = :deckId " +
           "AND rs.sessionDate >= :startDate " +
           "AND rs.sessionDate < :endDate " +
           "ORDER BY rs.sessionDate DESC")
    List<ReviewSession> findDeckSessionsInDateRange(@Param("deckId") Long deckId,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate deck popularity (session count)
     */
    @Query("SELECT rs.deck.id, COUNT(rs) FROM ReviewSession rs " +
           "WHERE rs.sessionDate >= :sinceDate " +
           "GROUP BY rs.deck.id " +
           "ORDER BY COUNT(rs) DESC")
    List<Object[]> calculateDeckPopularity(@Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Find long study sessions (for analysis)
     */
    @Query("SELECT rs FROM ReviewSession rs " +
           "WHERE rs.user.id = :userId " +
           "AND rs.durationMinutes >= :minDuration " +
           "ORDER BY rs.durationMinutes DESC")
    List<ReviewSession> findLongStudySessions(@Param("userId") Long userId,
                                            @Param("minDuration") int minDuration);

    /**
     * Find sessions by study mode and date range
     */
    @Query("SELECT rs FROM ReviewSession rs " +
           "WHERE rs.user.id = :userId " +
           "AND rs.studyMode = :studyMode " +
           "AND rs.sessionDate >= :startDate " +
           "AND rs.sessionDate < :endDate " +
           "ORDER BY rs.sessionDate DESC")
    List<ReviewSession> findSessionsByModeAndDateRange(@Param("userId") Long userId,
                                                      @Param("studyMode") StudyMode studyMode,
                                                      @Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);

    /**
     * Delete sessions when user is deleted
     */
    @Modifying
    @Query("DELETE FROM ReviewSession rs WHERE rs.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * Delete sessions when deck is deleted
     */
    @Modifying
    @Query("DELETE FROM ReviewSession rs WHERE rs.deck.id = :deckId")
    void deleteByDeckId(@Param("deckId") Long deckId);

    /**
     * Update session status for stale sessions
     */
    @Modifying
    @Query("UPDATE ReviewSession rs " +
           "SET rs.sessionStatus = 'ABANDONED' " +
           "WHERE rs.sessionStatus = 'IN_PROGRESS' " +
           "AND rs.sessionDate < :cutoffTime")
    int markStaleSessionsAsAbandoned(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Get study streak (consecutive days with sessions)
     */
    @Query(value = """
        WITH RECURSIVE date_series AS (
            SELECT CURRENT_DATE::date as study_date, 0 as days_back
            UNION ALL
            SELECT (study_date - INTERVAL '1 day')::date, days_back + 1
            FROM date_series
            WHERE days_back < 365
            AND EXISTS (
                SELECT 1 FROM review_sessions rs
                WHERE rs.user_id = :userId
                AND DATE(rs.session_date) = (study_date - INTERVAL '1 day')::date
                AND rs.session_status = 'COMPLETED'
            )
        )
        SELECT COUNT(*) - 1 FROM date_series
        """, nativeQuery = true)
    int calculateStudyStreak(@Param("userId") Long userId);

    /**
     * Get monthly statistics
     */
    @Query(value = """
        SELECT 
            EXTRACT(YEAR FROM rs.session_date) as year,
            EXTRACT(MONTH FROM rs.session_date) as month,
            COUNT(rs) as session_count,
            SUM(rs.cards_studied) as total_cards,
            AVG(rs.accuracy_rate) as avg_accuracy,
            SUM(rs.duration_minutes) as total_minutes
        FROM review_sessions rs
        WHERE rs.user_id = :userId
        AND rs.session_date >= :startDate
        GROUP BY EXTRACT(YEAR FROM rs.session_date), EXTRACT(MONTH FROM rs.session_date)
        ORDER BY year DESC, month DESC
        """, nativeQuery = true)
    List<Object[]> getMonthlyStatistics(@Param("userId") Long userId,
                                       @Param("startDate") LocalDateTime startDate);
}
