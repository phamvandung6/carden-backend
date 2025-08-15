package com.loopy.carden.repository;

import com.loopy.carden.entity.StudyState;
import com.loopy.carden.entity.StudyState.CardState;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Specifications for dynamic StudyState queries
 */
public class StudyStateSpecifications {

    /**
     * Filter by user ID
     */
    public static Specification<StudyState> hasUserId(Long userId) {
        return (root, query, criteriaBuilder) -> {
            if (userId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("user").get("id"), userId);
        };
    }

    /**
     * Filter by deck ID
     */
    public static Specification<StudyState> hasDeckId(Long deckId) {
        return (root, query, criteriaBuilder) -> {
            if (deckId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("deck").get("id"), deckId);
        };
    }

    /**
     * Filter by card state
     */
    public static Specification<StudyState> hasCardState(CardState cardState) {
        return (root, query, criteriaBuilder) -> {
            if (cardState == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("cardState"), cardState);
        };
    }

    /**
     * Filter cards that are due (dueDate <= now)
     */
    public static Specification<StudyState> isDue() {
        return (root, query, criteriaBuilder) -> {
            LocalDateTime now = LocalDateTime.now();
            return criteriaBuilder.lessThanOrEqualTo(root.get("dueDate"), now);
        };
    }

    /**
     * Filter cards due before specific date
     */
    public static Specification<StudyState> isDueBefore(LocalDateTime cutoffDate) {
        return (root, query, criteriaBuilder) -> {
            if (cutoffDate == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("dueDate"), cutoffDate);
        };
    }

    /**
     * Filter cards due after specific date
     */
    public static Specification<StudyState> isDueAfter(LocalDateTime startDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("dueDate"), startDate);
        };
    }

    /**
     * Filter by accuracy rate range
     */
    public static Specification<StudyState> hasAccuracyRateBetween(Double minAccuracy, Double maxAccuracy) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (minAccuracy != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("accuracyRate"), minAccuracy));
            }
            
            if (maxAccuracy != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("accuracyRate"), maxAccuracy));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Filter by minimum number of reviews
     */
    public static Specification<StudyState> hasMinimumReviews(Integer minReviews) {
        return (root, query, criteriaBuilder) -> {
            if (minReviews == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("totalReviews"), minReviews);
        };
    }

    /**
     * Filter by last review date range
     */
    public static Specification<StudyState> lastReviewedBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("lastReviewDate"), startDate));
            }
            
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("lastReviewDate"), endDate));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Filter by interval range (for debugging/analytics)
     */
    public static Specification<StudyState> hasIntervalBetween(Integer minInterval, Integer maxInterval) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (minInterval != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("intervalDays"), minInterval));
            }
            
            if (maxInterval != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("intervalDays"), maxInterval));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Filter by ease factor range
     */
    public static Specification<StudyState> hasEaseFactorBetween(Double minEase, Double maxEase) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (minEase != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("easeFactor"), minEase));
            }
            
            if (maxEase != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("easeFactor"), maxEase));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Filter by repetition count range
     */
    public static Specification<StudyState> hasRepetitionCountBetween(Integer minReps, Integer maxReps) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (minReps != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("repetitionCount"), minReps));
            }
            
            if (maxReps != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("repetitionCount"), maxReps));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Filter new cards only
     */
    public static Specification<StudyState> isNewCard() {
        return hasCardState(CardState.NEW);
    }

    /**
     * Filter learning cards only (both LEARNING and RELEARNING)
     */
    public static Specification<StudyState> isLearningCard() {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.or(
                criteriaBuilder.equal(root.get("cardState"), CardState.LEARNING),
                criteriaBuilder.equal(root.get("cardState"), CardState.RELEARNING)
            );
        };
    }

    /**
     * Filter review cards only
     */
    public static Specification<StudyState> isReviewCard() {
        return hasCardState(CardState.REVIEW);
    }

    /**
     * Complex filter: Due cards for a specific user and deck
     */
    public static Specification<StudyState> dueCardsForUserAndDeck(Long userId, Long deckId) {
        return hasUserId(userId)
                .and(hasDeckId(deckId))
                .and(isDue());
    }

    /**
     * Complex filter: Cards needing review for a user (due + learning)
     */
    public static Specification<StudyState> cardsNeedingReview(Long userId) {
        return hasUserId(userId)
                .and(isDue().or(isLearningCard()));
    }

    /**
     * Complex filter: Problem cards (low accuracy, many reviews)
     */
    public static Specification<StudyState> problemCards(Long userId, Double maxAccuracy, Integer minReviews) {
        return hasUserId(userId)
                .and(hasAccuracyRateBetween(null, maxAccuracy))
                .and(hasMinimumReviews(minReviews));
    }
}
