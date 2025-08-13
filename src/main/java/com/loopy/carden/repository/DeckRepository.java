package com.loopy.carden.repository;

import com.loopy.carden.entity.Deck;
import com.loopy.carden.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DeckRepository extends JpaRepository<Deck, Long>, JpaSpecificationExecutor<Deck> {
    List<Deck> findByUser(User user);

	Page<Deck> findByUser(User user, Pageable pageable);

	Page<Deck> findByUserAndVisibility(User user, Deck.Visibility visibility, Pageable pageable);

	@Query(
		value = "SELECT * FROM decks d " +
			"WHERE (:q IS NULL OR to_tsvector('english', coalesce(d.title,'') || ' ' || coalesce(d.description,'')) @@ plainto_tsquery('english', :q)) " +
			"AND (:publicOnly = FALSE OR d.is_public = TRUE) " +
			"AND (:topicId IS NULL OR d.topic_id = :topicId) " +
			"AND (:cefr IS NULL OR d.cefr_level = :cefr)",
		countQuery = "SELECT count(*) FROM decks d " +
			"WHERE (:q IS NULL OR to_tsvector('english', coalesce(d.title,'') || ' ' || coalesce(d.description,'')) @@ plainto_tsquery('english', :q)) " +
			"AND (:publicOnly = FALSE OR d.is_public = TRUE) " +
			"AND (:topicId IS NULL OR d.topic_id = :topicId) " +
			"AND (:cefr IS NULL OR d.cefr_level = :cefr)",
		nativeQuery = true
	)
	Page<Deck> searchFullTextNative(
		@Param("q") String q,
		@Param("publicOnly") boolean publicOnly,
		@Param("topicId") Long topicId,
		@Param("cefr") String cefr,
		Pageable pageable
	);
}


