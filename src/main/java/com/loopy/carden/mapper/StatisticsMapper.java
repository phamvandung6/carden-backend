package com.loopy.carden.mapper;

import com.loopy.carden.dto.statistics.PerformanceStatsDto;
import com.loopy.carden.dto.statistics.StudyStreakDto;
import com.loopy.carden.dto.statistics.UserStatisticsDto;
import com.loopy.carden.service.StudyStateService.UserStatistics;
import org.springframework.stereotype.Component;

/**
 * Mapper for Statistics DTOs
 */
@Component
public class StatisticsMapper {

    /**
     * Convert UserStatistics service class to DTO
     */
    public UserStatisticsDto toUserStatisticsDto(UserStatistics userStats, 
                                                 Integer currentStreak, 
                                                 java.util.Map<String, Long> cardStateDistribution,
                                                 Integer recentSessionCount,
                                                 Long recentStudyTimeMinutes,
                                                 java.time.LocalDateTime lastActivityDate,
                                                 Double averageSessionDuration,
                                                 Double studyEfficiency) {
        return UserStatisticsDto.builder()
                .userId(null) // Will be set by service
                .totalCards(userStats.getTotalCards())
                .totalSessions(null) // Will be set by service
                .completedSessions(null) // Will be set by service
                .totalStudyTimeMinutes(null) // Will be set by service
                .totalCardsStudied(userStats.getTotalReviews())
                .overallAccuracy(userStats.getAverageAccuracy())
                .currentStreak(currentStreak)
                .cardStateDistribution(cardStateDistribution)
                .recentSessionCount(recentSessionCount)
                .recentStudyTimeMinutes(recentStudyTimeMinutes)
                .lastActivityDate(lastActivityDate)
                .averageSessionDuration(averageSessionDuration)
                .studyEfficiency(studyEfficiency)
                .build();
    }

    /**
     * Create simplified statistics DTO for dashboard
     */
    public UserStatisticsDto.SimplifiedDto toSimplifiedDto(UserStatisticsDto fullStats) {
        return UserStatisticsDto.SimplifiedDto.builder()
                .userId(fullStats.getUserId())
                .totalCards(fullStats.getTotalCards())
                .currentStreak(fullStats.getCurrentStreak())
                .overallAccuracy(fullStats.getOverallAccuracy())
                .recentSessionCount(fullStats.getRecentSessionCount())
                .lastActivityDate(fullStats.getLastActivityDate())
                .build();
    }
}
