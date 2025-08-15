package com.loopy.carden.repository;

import com.loopy.carden.entity.Card;
import com.loopy.carden.entity.Deck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {

    // Basic deck-based queries
    Page<Card> findByDeck(Deck deck, Pageable pageable);
    
    List<Card> findByDeckOrderByDisplayOrder(Deck deck);
    
    long countByDeck(Deck deck);
    
    long countByDeckId(Long deckId);

    // Duplicate detection
    @Query("SELECT c FROM Card c WHERE c.deck = :deck AND c.uniqueKey = :uniqueKey")
    Optional<Card> findByDeckAndUniqueKey(@Param("deck") Deck deck, @Param("uniqueKey") String uniqueKey);
    
    @Query("SELECT c FROM Card c WHERE c.deck = :deck AND c.uniqueKey = :uniqueKey AND c.id != :excludeId")
    Optional<Card> findDuplicateCard(@Param("deck") Deck deck, @Param("uniqueKey") String uniqueKey, @Param("excludeId") Long excludeId);

    // Full-text search using PostgreSQL
    @Query(
        value = "SELECT * FROM cards c " +
            "WHERE c.deck_id = :deckId " +
            "AND (:q IS NULL OR to_tsvector('english', coalesce(c.front,'') || ' ' || coalesce(c.back,'') || ' ' || coalesce(c.ipa_pronunciation,'')) @@ plainto_tsquery('english', :q)) " +
            "AND (:difficulty IS NULL OR c.difficulty = :difficulty)",
        countQuery = "SELECT count(*) FROM cards c " +
            "WHERE c.deck_id = :deckId " +
            "AND (:q IS NULL OR to_tsvector('english', coalesce(c.front,'') || ' ' || coalesce(c.back,'') || ' ' || coalesce(c.ipa_pronunciation,'')) @@ plainto_tsquery('english', :q)) " +
            "AND (:difficulty IS NULL OR c.difficulty = :difficulty)",
        nativeQuery = true
    )
    Page<Card> searchFullTextNative(
        @Param("deckId") Long deckId,
        @Param("q") String q,
        @Param("difficulty") String difficulty,
        Pageable pageable
    );

    // Tag-based search using JSONB
    @Query(
        value = "SELECT * FROM cards c " +
            "WHERE c.deck_id = :deckId " +
            "AND (:tag IS NULL OR c.tags @> :tagJson)",
        nativeQuery = true
    )
    List<Card> findByDeckAndTag(@Param("deckId") Long deckId, @Param("tag") String tag, @Param("tagJson") String tagJson);

    // Statistics queries
    @Query("SELECT AVG(CAST(c.difficulty AS int)) FROM Card c WHERE c.deck = :deck")
    Double getAverageDifficultyByDeck(@Param("deck") Deck deck);
    
    @Query("SELECT COUNT(c) FROM Card c WHERE c.deck = :deck AND c.difficulty = :difficulty")
    long countByDeckAndDifficulty(@Param("deck") Deck deck, @Param("difficulty") Card.Difficulty difficulty);

    // Advanced search with multiple criteria
    @Query(
        value = "SELECT * FROM cards c " +
            "WHERE c.deck_id = :deckId " +
            "AND (:q IS NULL OR " +
            "    to_tsvector('english', coalesce(c.front,'') || ' ' || coalesce(c.back,'') || ' ' || " +
            "    coalesce(c.ipa_pronunciation,'') || ' ' || " +
            "    coalesce(array_to_string(ARRAY(SELECT jsonb_array_elements_text(c.examples)), ' '),'') || ' ' || " +
            "    coalesce(array_to_string(ARRAY(SELECT jsonb_array_elements_text(c.synonyms)), ' '),'') || ' ' || " +
            "    coalesce(array_to_string(ARRAY(SELECT jsonb_array_elements_text(c.antonyms)), ' '),'')) " +
            "    @@ plainto_tsquery('english', :q)) " +
            "AND (:difficulty IS NULL OR c.difficulty = :difficulty) " +
            "AND (:hasImage IS NULL OR (:hasImage = true AND c.image_url IS NOT NULL) OR (:hasImage = false AND c.image_url IS NULL)) " +
            "AND (:hasAudio IS NULL OR (:hasAudio = true AND c.audio_url IS NOT NULL) OR (:hasAudio = false AND c.audio_url IS NULL))",
        countQuery = "SELECT count(*) FROM cards c " +
            "WHERE c.deck_id = :deckId " +
            "AND (:q IS NULL OR " +
            "    to_tsvector('english', coalesce(c.front,'') || ' ' || coalesce(c.back,'') || ' ' || " +
            "    coalesce(c.ipa_pronunciation,'') || ' ' || " +
            "    coalesce(array_to_string(ARRAY(SELECT jsonb_array_elements_text(c.examples)), ' '),'') || ' ' || " +
            "    coalesce(array_to_string(ARRAY(SELECT jsonb_array_elements_text(c.synonyms)), ' '),'') || ' ' || " +
            "    coalesce(array_to_string(ARRAY(SELECT jsonb_array_elements_text(c.antonyms)), ' '),'')) " +
            "    @@ plainto_tsquery('english', :q)) " +
            "AND (:difficulty IS NULL OR c.difficulty = :difficulty) " +
            "AND (:hasImage IS NULL OR (:hasImage = true AND c.image_url IS NOT NULL) OR (:hasImage = false AND c.image_url IS NULL)) " +
            "AND (:hasAudio IS NULL OR (:hasAudio = true AND c.audio_url IS NOT NULL) OR (:hasAudio = false AND c.audio_url IS NULL))",
        nativeQuery = true
    )
    Page<Card> advancedSearch(
        @Param("deckId") Long deckId,
        @Param("q") String q,
        @Param("difficulty") String difficulty,
        @Param("hasImage") Boolean hasImage,
        @Param("hasAudio") Boolean hasAudio,
        Pageable pageable
    );
}
