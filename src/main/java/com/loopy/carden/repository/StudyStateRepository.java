package com.loopy.carden.repository;

import com.loopy.carden.entity.Card;
import com.loopy.carden.entity.StudyState;
import com.loopy.carden.entity.StudyState.CardState;
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
public interface StudyStateRepository extends JpaRepository<StudyState, Long> {

    /**
     * Find study state by user and card
     */
    Optional<StudyState> findByUserIdAndCardId(Long userId, Long cardId);

    /**
     * Find all due cards for a user (core SRS query)
     */
    @Query("SELECT s FROM StudyState s " +
           "WHERE s.user.id = :userId " +
           "AND s.dueDate <= :now " +
           "ORDER BY s.dueDate ASC, s.cardState ASC")
    Page<StudyState> findDueCardsByUser(@Param("userId") Long userId, 
                                       @Param("now") LocalDateTime now, 
                                       Pageable pageable);

    /**
     * Find all due cards for a user in a specific deck
     */
    @Query("SELECT s FROM StudyState s " +
           "WHERE s.user.id = :userId " +
           "AND s.deck.id = :deckId " +
           "AND s.dueDate <= :now " +
           "ORDER BY s.dueDate ASC")
    List<StudyState> findDueCardsByUserAndDeck(@Param("userId") Long userId,
                                              @Param("deckId") Long deckId,
                                              @Param("now") LocalDateTime now);

    /**
     * Count due cards for a user
     */
    @Query("SELECT COUNT(s) FROM StudyState s " +
           "WHERE s.user.id = :userId " +
           "AND s.dueDate <= :now")
    Long countDueCardsByUser(@Param("userId") Long userId, 
                            @Param("now") LocalDateTime now);

    /**
     * Find cards by state for a user
     */
    @Query("SELECT s FROM StudyState s " +
           "WHERE s.user.id = :userId " +
           "AND s.cardState = :cardState " +
           "ORDER BY s.dueDate ASC")
    Page<StudyState> findByUserIdAndCardState(@Param("userId") Long userId,
                                             @Param("cardState") CardState cardState,
                                             Pageable pageable);

    /**
     * Find new cards for a user (cards never studied)
     */
    @Query("SELECT s FROM StudyState s " +
           "WHERE s.user.id = :userId " +
           "AND s.cardState = 'NEW' " +
           "ORDER BY s.createdAt ASC")
    Page<StudyState> findNewCardsByUser(@Param("userId") Long userId, 
                                       Pageable pageable);

    /**
     * Find learning cards (failed cards in learning phase)
     */
    @Query("SELECT s FROM StudyState s " +
           "WHERE s.user.id = :userId " +
           "AND s.cardState IN ('LEARNING', 'RELEARNING') " +
           "AND s.dueDate <= :now " +
           "ORDER BY s.dueDate ASC")
    List<StudyState> findLearningCardsByUser(@Param("userId") Long userId,
                                            @Param("now") LocalDateTime now);

    /**
     * Find all study states for a deck (for deck statistics)
     */
    @Query("SELECT s FROM StudyState s " +
           "WHERE s.deck.id = :deckId " +
           "ORDER BY s.createdAt DESC")
    List<StudyState> findByDeckId(@Param("deckId") Long deckId);

    /**
     * Calculate user's overall statistics
     */
    @Query("SELECT " +
           "COUNT(s), " +
           "AVG(s.accuracyRate), " +
           "SUM(s.totalReviews), " +
           "SUM(s.correctReviews) " +
           "FROM StudyState s " +
           "WHERE s.user.id = :userId")
    Object[] calculateUserStatistics(@Param("userId") Long userId);

    /**
     * Find cards reviewed today
     */
    @Query("SELECT s FROM StudyState s " +
           "WHERE s.user.id = :userId " +
           "AND s.lastReviewDate >= :startOfDay " +
           "AND s.lastReviewDate < :endOfDay " +
           "ORDER BY s.lastReviewDate DESC")
    List<StudyState> findCardsReviewedToday(@Param("userId") Long userId,
                                           @Param("startOfDay") LocalDateTime startOfDay,
                                           @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * Delete study states when a card is deleted
     */
    @Modifying
    @Query("DELETE FROM StudyState s WHERE s.card.id = :cardId")
    void deleteByCardId(@Param("cardId") Long cardId);

    /**
     * Delete study states when a deck is deleted
     */
    @Modifying
    @Query("DELETE FROM StudyState s WHERE s.deck.id = :deckId")
    void deleteByDeckId(@Param("deckId") Long deckId);

    /**
     * Delete study states when a user is deleted
     */
    @Modifying
    @Query("DELETE FROM StudyState s WHERE s.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * Batch update accuracy rates (for maintenance tasks)
     */
    @Modifying
    @Query("UPDATE StudyState s " +
           "SET s.accuracyRate = " +
           "CASE WHEN s.totalReviews > 0 " +
           "THEN (s.correctReviews * 100.0 / s.totalReviews) " +
           "ELSE 0.0 END")
    int updateAllAccuracyRates();

    /**
     * Find overdue cards (for debugging/maintenance)
     */
    @Query("SELECT s FROM StudyState s " +
           "WHERE s.dueDate < :cutoffDate " +
           "ORDER BY s.dueDate ASC")
    List<StudyState> findOverdueCards(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Count total cards in different states for a user
     */
    @Query("SELECT s.cardState, COUNT(s) " +
           "FROM StudyState s " +
           "WHERE s.user.id = :userId " +
           "GROUP BY s.cardState")
    List<Object[]> countCardsByState(@Param("userId") Long userId);

    /**
     * Count total study states for a user
     */
    @Query("SELECT COUNT(s) FROM StudyState s WHERE s.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    /**
     * Find cards that don't have study states yet for a user (truly new cards)
     * This should be used to find cards available for first-time study
     */
    @Query("SELECT c FROM Card c " +
           "WHERE c.deck.user.id = :userId " +
           "AND c.id NOT IN (SELECT s.card.id FROM StudyState s WHERE s.user.id = :userId) " +
           "ORDER BY c.createdAt ASC")
    Page<Card> findCardsWithoutStudyState(@Param("userId") Long userId, Pageable pageable);

    /**
     * Count cards that don't have study states yet for a user
     */
    @Query("SELECT COUNT(c) FROM Card c " +
           "WHERE c.deck.user.id = :userId " +
           "AND c.id NOT IN (SELECT s.card.id FROM StudyState s WHERE s.user.id = :userId)")
    long countCardsWithoutStudyState(@Param("userId") Long userId);
}
