package com.loopy.carden.dto.practice;

import com.loopy.carden.entity.ReviewSession.SessionStats;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for practice session summary
 */
@Data
@Builder
public class SessionSummaryDto {
    private Long sessionId;
    private Integer totalCards;
    private Integer correctCards;
    private Double finalAccuracy;
    private Integer durationMinutes;
    private Integer newCards;
    private Integer reviewCards;
    private Integer relearningCards;
    private SessionStats sessionStats;
    private LocalDateTime completedAt;
    
    // Next study time information
    private LocalDateTime nextAvailableStudyTime;
    private Long minutesUntilNextCard;
    private Boolean canStudyNow;
}
