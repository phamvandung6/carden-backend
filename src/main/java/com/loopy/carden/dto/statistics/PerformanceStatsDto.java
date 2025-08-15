package com.loopy.carden.dto.statistics;

import com.loopy.carden.service.StatisticsService.AccuracyPoint;
import com.loopy.carden.service.StatisticsService.WeeklyTrend;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for performance statistics over time
 */
@Data
@Builder
public class PerformanceStatsDto {
    private Long userId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer totalSessions;
    private Double averageAccuracy;
    private Long totalStudyTime; // minutes
    private Integer cardsStudied;
    private Map<String, Double> dailyAverages;
    private List<WeeklyTrend> weeklyTrends;
    private List<AccuracyPoint> accuracyTrend;
    private Double improvementRate; // Accuracy improvement per day
    private Double consistency; // Lower variance = higher consistency
}
