package com.loopy.carden.dto.studystate;

import com.loopy.carden.entity.StudyState.CardState;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for StudyState entity
 */
@Data
public class StudyStateResponseDto {
    private Long id;
    private Long cardId;
    private Long userId;
    private Long deckId;
    private Integer repetitionCount;
    private Double easeFactor;
    private Integer intervalDays;
    private LocalDateTime dueDate;
    private CardState cardState;
    private LocalDateTime lastReviewDate;
    private Integer lastScore;
    private Integer totalReviews;
    private Integer correctReviews;
    private Double accuracyRate;
    private Integer consecutiveFailures;
    private Integer currentLearningStep;
    private Boolean isLeech;
    private LocalDateTime graduatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Computed fields
    private Boolean isDue;
    private Boolean isNew;
    private Boolean isLearning;
    private Boolean isInReview;

    /**
     * Simplified DTO for practice sessions (reduced data transfer)
     */
    @Data
    public static class SimplifiedDto {
        private Long id;
        private Long cardId;
        private CardState cardState;
        private LocalDateTime dueDate;
        private Integer intervalDays;
        private Double accuracyRate;
        private Integer totalReviews;
        private Boolean isDue;
        private Boolean isNew;
        private Boolean isLearning;
    }
}
