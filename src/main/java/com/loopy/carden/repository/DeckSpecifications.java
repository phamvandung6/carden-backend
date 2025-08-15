package com.loopy.carden.repository;

import com.loopy.carden.entity.Deck;
import org.springframework.data.jpa.domain.Specification;

public final class DeckSpecifications {

    private DeckSpecifications() {}

    public static Specification<Deck> titleOrDescriptionContains(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return cb.conjunction();
            }
            String like = "%" + text.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("description")), like)
            );
        };
    }

    public static Specification<Deck> hasTopicId(Long topicId) {
        return (root, query, cb) -> {
            if (topicId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("topic").get("id"), topicId);
        };
    }

    public static Specification<Deck> hasVisibility(Deck.Visibility visibility) {
        return (root, query, cb) -> visibility == null ? cb.conjunction() : cb.equal(root.get("visibility"), visibility);
    }

    public static Specification<Deck> hasCefr(Deck.CEFRLevel cefr) {
        return (root, query, cb) -> cefr == null ? cb.conjunction() : cb.equal(root.get("cefrLevel"), cefr);
    }

    public static Specification<Deck> isPublic(boolean publicOnly) {
        return (root, query, cb) -> publicOnly ? cb.equal(root.get("visibility"), Deck.Visibility.PUBLIC) : cb.conjunction();
    }
}


