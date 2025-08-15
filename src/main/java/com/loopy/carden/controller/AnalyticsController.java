package com.loopy.carden.controller;

import com.loopy.carden.dto.StandardResponse;
import com.loopy.carden.dto.statistics.PerformanceStatsDto;
import com.loopy.carden.dto.statistics.StudyStreakDto;
import com.loopy.carden.dto.statistics.UserStatisticsDto;
import com.loopy.carden.entity.User;
import com.loopy.carden.service.InsightsService;
import com.loopy.carden.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST API for analytics and statistics
 * 
 * NOTE: Statistics endpoints have been consolidated here from PracticeController
 * for better organization. All analytics/statistics functionality is now centralized.
 */
@RestController
@RequestMapping("/v1/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics", description = "User analytics and performance statistics")
public class AnalyticsController {

    private final StatisticsService statisticsService;
    private final InsightsService insightsService;

    @GetMapping("/overview")
    @Operation(summary = "Get user statistics overview",
               description = "Returns comprehensive user statistics including study progress, accuracy, streaks, and card distributions")
    public ResponseEntity<StandardResponse<UserStatisticsDto>> getUserOverview(
            @AuthenticationPrincipal User user) {
        
        UserStatisticsDto stats = statisticsService.getUserStatistics(user.getId());
        
        log.debug("Retrieved overview statistics for user {}: {} total cards, {}% accuracy", 
                user.getId(), stats.getTotalCards(), stats.getOverallAccuracy());
        
        return ResponseEntity.ok(StandardResponse.success(stats));
    }

    @GetMapping("/overview/simplified")
    @Operation(summary = "Get simplified user statistics",
               description = "Returns basic user statistics for dashboard display with key metrics only")
    public ResponseEntity<StandardResponse<Map<String, Object>>> getSimplifiedOverview(
            @AuthenticationPrincipal User user) {
        
        UserStatisticsDto fullStats = statisticsService.getUserStatistics(user.getId());
        
        // Create simplified version with only essential metrics
        Map<String, Object> simplified = Map.of(
                "totalCards", fullStats.getTotalCards(),
                "totalSessions", fullStats.getTotalSessions(),
                "overallAccuracy", Math.round(fullStats.getOverallAccuracy() * 100.0) / 100.0,
                "currentStreak", fullStats.getCurrentStreak(),
                "totalStudyTimeMinutes", fullStats.getTotalStudyTimeMinutes(),
                "totalCardsStudied", fullStats.getTotalCardsStudied(),
                "recentSessionCount", fullStats.getRecentSessionCount(),
                "cardStates", fullStats.getCardStateDistribution()
        );
        
        return ResponseEntity.ok(StandardResponse.success(simplified));
    }

    @GetMapping("/performance")
    @Operation(summary = "Get performance statistics over time",
               description = "Returns detailed performance statistics for a specified time period with trends and accuracy data")
    public ResponseEntity<StandardResponse<PerformanceStatsDto>> getPerformanceStats(
            @Parameter(description = "Start date (ISO format: 2024-01-01T00:00:00)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            
            @Parameter(description = "End date (ISO format: 2024-12-31T23:59:59)")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            
            @Parameter(description = "Time period preset (7d, 30d, 90d, 1y)")
            @RequestParam(required = false, defaultValue = "30d") String period,
            
            @AuthenticationPrincipal User user) {
        
        // Handle preset periods if specific dates not provided
        if (startDate == null || endDate == null) {
            endDate = LocalDateTime.now();
            startDate = switch (period.toLowerCase()) {
                case "7d" -> endDate.minusDays(7);
                case "30d" -> endDate.minusDays(30);
                case "90d" -> endDate.minusDays(90);
                case "1y" -> endDate.minusYears(1);
                default -> endDate.minusDays(30);
            };
        }
        
        PerformanceStatsDto stats = statisticsService.getPerformanceStats(user.getId(), startDate, endDate);
        
        log.debug("Retrieved performance statistics for user {} from {} to {}: {} sessions", 
                user.getId(), startDate, endDate, stats.getTotalSessions());
        
        return ResponseEntity.ok(StandardResponse.success(stats));
    }

    @GetMapping("/streaks")
    @Operation(summary = "Get study streak information",
               description = "Returns current and historical study streak data with milestones")
    public ResponseEntity<StandardResponse<StudyStreakDto>> getStudyStreaks(
            @AuthenticationPrincipal User user) {
        
        StudyStreakDto streaks = statisticsService.getStudyStreak(user.getId());
        
        log.debug("Retrieved streak data for user {}: current {} days, longest {} days", 
                user.getId(), streaks.getCurrentStreak(), streaks.getLongestStreak());
        
        return ResponseEntity.ok(StandardResponse.success(streaks));
    }

    @GetMapping("/deck/{deckId}")
    @Operation(summary = "Get deck-specific statistics",
               description = "Returns performance statistics for a specific deck including progress and difficulty analysis")
    public ResponseEntity<StandardResponse<Map<String, Object>>> getDeckStatistics(
            @PathVariable Long deckId,
            @AuthenticationPrincipal User user) {
        
        Map<String, Object> deckStats = statisticsService.getDeckStatistics(user.getId(), deckId);
        
        log.debug("Retrieved deck statistics for user {} deck {}: {} total cards", 
                user.getId(), deckId, deckStats.get("totalCards"));
        
        return ResponseEntity.ok(StandardResponse.success(deckStats));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get analytics summary for dashboard",
               description = "Returns a comprehensive summary suitable for main dashboard display")
    public ResponseEntity<StandardResponse<Map<String, Object>>> getAnalyticsSummary(
            @AuthenticationPrincipal User user) {
        
        UserStatisticsDto overview = statisticsService.getUserStatistics(user.getId());
        StudyStreakDto streaks = statisticsService.getStudyStreak(user.getId());
        
        // Performance for last 30 days
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        PerformanceStatsDto performance = statisticsService.getPerformanceStats(user.getId(), thirtyDaysAgo, now);
        
        Map<String, Object> summary = Map.of(
                "overview", Map.of(
                        "totalCards", overview.getTotalCards(),
                        "totalSessions", overview.getTotalSessions(),
                        "overallAccuracy", Math.round(overview.getOverallAccuracy() * 100.0) / 100.0,
                        "totalStudyTimeMinutes", overview.getTotalStudyTimeMinutes()
                ),
                "streaks", Map.of(
                        "current", streaks.getCurrentStreak(),
                        "longest", streaks.getLongestStreak(),
                        "nextMilestone", streaks.getNextMilestone(),
                        "daysToMilestone", streaks.getDaysToMilestone()
                ),
                "recent", Map.of(
                        "sessionsLast30Days", performance.getTotalSessions(),
                        "accuracyLast30Days", Math.round(performance.getAverageAccuracy() * 100.0) / 100.0,
                        "studyTimeLast30Days", performance.getTotalStudyTime(),
                        "cardsStudiedLast30Days", performance.getCardsStudied()
                ),
                "distribution", overview.getCardStateDistribution()
        );
        
        log.debug("Generated analytics summary for user {}", user.getId());
        
        return ResponseEntity.ok(StandardResponse.success(summary));
    }

    @GetMapping("/insights")
    @Operation(summary = "Get personalized study insights",
               description = "Returns AI-generated insights and recommendations based on user's study patterns and performance")
    public ResponseEntity<StandardResponse<List<InsightsService.StudyInsight>>> getStudyInsights(
            @AuthenticationPrincipal User user) {
        
        List<InsightsService.StudyInsight> insights = insightsService.generateInsights(user.getId());
        
        log.debug("Generated {} insights for user {}", insights.size(), user.getId());
        
        return ResponseEntity.ok(StandardResponse.success(insights));
    }
}
