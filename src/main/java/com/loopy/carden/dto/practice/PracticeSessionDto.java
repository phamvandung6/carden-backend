package com.loopy.carden.dto.practice;

import com.loopy.carden.entity.ReviewSession.StudyMode;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for practice session information
 */
@Data
@Builder
public class PracticeSessionDto {
    private Long sessionId;
    private StudyMode studyMode;
    private Long deckId;
    private LocalDateTime startTime;
    private Integer cardsStudied;
    private Integer cardsCorrect;
    private Double currentAccuracy;
    private Integer durationMinutes;
    private Integer dueCardsCount;
    private Integer maxNewCards;
    private Integer maxReviewCards;
    private Boolean includeNewCards;
    private Boolean includeReviewCards;
    private Boolean includeLearningCards;
}
