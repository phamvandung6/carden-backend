package com.loopy.carden.dto.session;

import com.loopy.carden.entity.ReviewSession.SessionStats;
import com.loopy.carden.entity.ReviewSession.SessionStatus;
import com.loopy.carden.entity.ReviewSession.StudyMode;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for ReviewSession entity
 */
@Data
public class ReviewSessionResponseDto {
    private Long id;
    private Long userId;
    private Long deckId;
    private String deckTitle;
    private LocalDateTime sessionDate;
    private Integer durationMinutes;
    private Integer cardsStudied;
    private Integer cardsCorrect;
    private Integer newCards;
    private Integer reviewCards;
    private Integer relearningCards;
    private Double accuracyRate;
    private StudyMode studyMode;
    private SessionStatus sessionStatus;
    private SessionStats sessionStats;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Computed fields
    private Boolean isCompleted;
    private Boolean isInProgress;

    /**
     * Summary DTO for statistics and lists
     */
    @Data
    public static class SummaryDto {
        private Long id;
        private LocalDateTime sessionDate;
        private Integer durationMinutes;
        private Integer cardsStudied;
        private Double accuracyRate;
        private StudyMode studyMode;
        private SessionStatus sessionStatus;
        private String deckTitle;
    }
}
