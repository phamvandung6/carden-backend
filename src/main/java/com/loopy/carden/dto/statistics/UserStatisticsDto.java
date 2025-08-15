package com.loopy.carden.dto.statistics;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for comprehensive user statistics
 */
@Data
@Builder
public class UserStatisticsDto {
    private Long userId;
    private Long totalCards;
    private Long totalSessions;
    private Long completedSessions;
    private Long totalStudyTimeMinutes;
    private Long totalCardsStudied;
    private Double overallAccuracy;
    private Integer currentStreak;
    private Map<String, Long> cardStateDistribution;
    private Integer recentSessionCount; // Last 30 days
    private Long recentStudyTimeMinutes; // Last 30 days
    private LocalDateTime lastActivityDate;
    private Double averageSessionDuration;
    private Double studyEfficiency; // Cards per minute

    /**
     * Simplified DTO for dashboard display
     */
    @Data
    @Builder
    public static class SimplifiedDto {
        private Long userId;
        private Long totalCards;
        private Integer currentStreak;
        private Double overallAccuracy;
        private Integer recentSessionCount;
        private LocalDateTime lastActivityDate;
    }
}
