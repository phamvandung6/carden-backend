package com.loopy.carden.repository;

import com.loopy.carden.entity.Card;
import com.loopy.carden.entity.Deck;
import org.springframework.data.jpa.domain.Specification;

public final class CardSpecifications {

    private CardSpecifications() {}

    public static Specification<Card> belongsToDeck(Deck deck) {
        return (root, query, cb) -> deck == null ? cb.conjunction() : cb.equal(root.get("deck"), deck);
    }

    public static Specification<Card> frontOrBackContains(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return cb.conjunction();
            }
            String like = "%" + text.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("front")), like),
                    cb.like(cb.lower(root.get("back")), like),
                    cb.like(cb.lower(root.get("ipaPronunciation")), like)
            );
        };
    }

    public static Specification<Card> hasDifficulty(Card.Difficulty difficulty) {
        return (root, query, cb) -> difficulty == null ? cb.conjunction() : cb.equal(root.get("difficulty"), difficulty);
    }

    public static Specification<Card> hasUniqueKey(String uniqueKey) {
        return (root, query, cb) -> uniqueKey == null ? cb.conjunction() : cb.equal(root.get("uniqueKey"), uniqueKey);
    }

    public static Specification<Card> excludeId(Long excludeId) {
        return (root, query, cb) -> excludeId == null ? cb.conjunction() : cb.notEqual(root.get("id"), excludeId);
    }
}
