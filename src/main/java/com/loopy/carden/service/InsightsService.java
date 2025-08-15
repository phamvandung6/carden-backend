package com.loopy.carden.service;

import com.loopy.carden.dto.statistics.PerformanceStatsDto;
import com.loopy.carden.dto.statistics.UserStatisticsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for generating study insights and recommendations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InsightsService {

    private final StatisticsService statisticsService;

    /**
     * Generate study insights for a user
     */
    public List<StudyInsight> generateInsights(Long userId) {
        List<StudyInsight> insights = new ArrayList<>();
        
        UserStatisticsDto stats = statisticsService.getUserStatistics(userId);
        
        // Performance insights
        insights.addAll(generatePerformanceInsights(userId, stats));
        
        // Streak insights
        insights.addAll(generateStreakInsights(stats));
        
        // Study pattern insights
        insights.addAll(generateStudyPatternInsights(userId, stats));
        
        // Motivational insights
        insights.addAll(generateMotivationalInsights(stats));
        
        return insights;
    }

    /**
     * Generate performance-related insights
     */
    private List<StudyInsight> generatePerformanceInsights(Long userId, UserStatisticsDto stats) {
        List<StudyInsight> insights = new ArrayList<>();
        
        // Get last 30 days performance
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        PerformanceStatsDto recentPerf = statisticsService.getPerformanceStats(userId, thirtyDaysAgo, now);
        
        // Accuracy insights
        if (stats.getOverallAccuracy() >= 90.0) {
            insights.add(StudyInsight.builder()
                    .type(InsightType.PERFORMANCE)
                    .title("Excellent Accuracy!")
                    .message("Your overall accuracy is " + Math.round(stats.getOverallAccuracy()) + "%. You're mastering your cards well!")
                    .priority(InsightPriority.POSITIVE)
                    .actionable(false)
                    .build());
        } else if (stats.getOverallAccuracy() < 70.0) {
            insights.add(StudyInsight.builder()
                    .type(InsightType.PERFORMANCE)
                    .title("Focus on Review")
                    .message("Your accuracy is " + Math.round(stats.getOverallAccuracy()) + "%. Try reviewing difficult cards more frequently.")
                    .priority(InsightPriority.MEDIUM)
                    .actionable(true)
                    .recommendation("Consider using multiple study modes to reinforce learning")
                    .build());
        }
        
        // Recent activity insights
        if (recentPerf.getTotalSessions() == 0) {
            insights.add(StudyInsight.builder()
                    .type(InsightType.ACTIVITY)
                    .title("Ready for a Study Session?")
                    .message("You haven't studied in the past 30 days. Let's get back into the habit!")
                    .priority(InsightPriority.HIGH)
                    .actionable(true)
                    .recommendation("Start with a short 10-15 minute session")
                    .build());
        } else if (recentPerf.getTotalSessions() < 5) {
            insights.add(StudyInsight.builder()
                    .type(InsightType.ACTIVITY)
                    .title("Consistency is Key")
                    .message("You've studied " + recentPerf.getTotalSessions() + " times in the past 30 days. Try to study more regularly for better retention.")
                    .priority(InsightPriority.MEDIUM)
                    .actionable(true)
                    .recommendation("Aim for at least 15-20 minutes of study per day")
                    .build());
        }
        
        return insights;
    }

    /**
     * Generate streak-related insights
     */
    private List<StudyInsight> generateStreakInsights(UserStatisticsDto stats) {
        List<StudyInsight> insights = new ArrayList<>();
        
        if (stats.getCurrentStreak() == 0) {
            insights.add(StudyInsight.builder()
                    .type(InsightType.STREAK)
                    .title("Start Your Streak!")
                    .message("Begin a new study streak today. Consistency is the key to language learning success.")
                    .priority(InsightPriority.MEDIUM)
                    .actionable(true)
                    .recommendation("Study for just 10 minutes to start your streak")
                    .build());
        } else if (stats.getCurrentStreak() >= 7) {
            insights.add(StudyInsight.builder()
                    .type(InsightType.STREAK)
                    .title("Great Streak!")
                    .message("You're on a " + stats.getCurrentStreak() + "-day study streak. Keep up the excellent work!")
                    .priority(InsightPriority.POSITIVE)
                    .actionable(false)
                    .build());
        } else if (stats.getCurrentStreak() >= 3) {
            insights.add(StudyInsight.builder()
                    .type(InsightType.STREAK)
                    .title("Building Momentum")
                    .message("You're on a " + stats.getCurrentStreak() + "-day streak. You're building great study habits!")
                    .priority(InsightPriority.POSITIVE)
                    .actionable(false)
                    .build());
        }
        
        return insights;
    }

    /**
     * Generate study pattern insights
     */
    private List<StudyInsight> generateStudyPatternInsights(Long userId, UserStatisticsDto stats) {
        List<StudyInsight> insights = new ArrayList<>();
        
        // Cards distribution insights
        Long totalCards = stats.getTotalCards();
        Long studiedCards = stats.getTotalCardsStudied();
        
        if (totalCards > 0) {
            double studiedPercentage = (studiedCards.doubleValue() / totalCards.doubleValue()) * 100;
            
            if (studiedPercentage < 25) {
                insights.add(StudyInsight.builder()
                        .type(InsightType.PROGRESS)
                        .title("Explore Your Cards")
                        .message("You've studied " + Math.round(studiedPercentage) + "% of your cards. There's lots more to discover!")
                        .priority(InsightPriority.MEDIUM)
                        .actionable(true)
                        .recommendation("Try studying some new cards to expand your vocabulary")
                        .build());
            } else if (studiedPercentage >= 75) {
                insights.add(StudyInsight.builder()
                        .type(InsightType.PROGRESS)
                        .title("Great Coverage!")
                        .message("You've studied " + Math.round(studiedPercentage) + "% of your cards. Excellent progress!")
                        .priority(InsightPriority.POSITIVE)
                        .actionable(false)
                        .build());
            }
        }
        
        // Study efficiency insights
        if (stats.getStudyEfficiency() != null) {
            double efficiency = stats.getStudyEfficiency();
            if (efficiency > 2.0) { // More than 2 cards per minute
                insights.add(StudyInsight.builder()
                        .type(InsightType.EFFICIENCY)
                        .title("Efficient Learner")
                        .message("You're studying at " + String.format("%.1f", efficiency) + " cards per minute. Great pace!")
                        .priority(InsightPriority.POSITIVE)
                        .actionable(false)
                        .build());
            } else if (efficiency < 0.5) { // Less than 0.5 cards per minute
                insights.add(StudyInsight.builder()
                        .type(InsightType.EFFICIENCY)
                        .title("Take Your Time")
                        .message("You're studying at " + String.format("%.1f", efficiency) + " cards per minute. Quality over speed!")
                        .priority(InsightPriority.NEUTRAL)
                        .actionable(false)
                        .build());
            }
        }
        
        return insights;
    }

    /**
     * Generate motivational insights
     */
    private List<StudyInsight> generateMotivationalInsights(UserStatisticsDto stats) {
        List<StudyInsight> insights = new ArrayList<>();
        
        // Total study time achievements
        long totalHours = stats.getTotalStudyTimeMinutes() / 60;
        
        if (totalHours >= 100) {
            insights.add(StudyInsight.builder()
                    .type(InsightType.ACHIEVEMENT)
                    .title("Century Club!")
                    .message("You've studied for over " + totalHours + " hours total. That's dedication!")
                    .priority(InsightPriority.POSITIVE)
                    .actionable(false)
                    .build());
        } else if (totalHours >= 50) {
            insights.add(StudyInsight.builder()
                    .type(InsightType.ACHIEVEMENT)
                    .title("Halfway to 100!")
                    .message("You've studied for " + totalHours + " hours. Keep going to reach 100 hours!")
                    .priority(InsightPriority.POSITIVE)
                    .actionable(false)
                    .build());
        } else if (totalHours >= 10) {
            insights.add(StudyInsight.builder()
                    .type(InsightType.ACHIEVEMENT)
                    .title("Building Your Foundation")
                    .message("You've studied for " + totalHours + " hours. Every minute counts!")
                    .priority(InsightPriority.POSITIVE)
                    .actionable(false)
                    .build());
        }
        
        // Session count achievements
        if (stats.getTotalSessions() >= 100) {
            insights.add(StudyInsight.builder()
                    .type(InsightType.ACHIEVEMENT)
                    .title("Session Master!")
                    .message("You've completed " + stats.getTotalSessions() + " study sessions. Consistency pays off!")
                    .priority(InsightPriority.POSITIVE)
                    .actionable(false)
                    .build());
        }
        
        return insights;
    }

    /**
     * Data class for study insights
     */
    public static class StudyInsight {
        private InsightType type;
        private String title;
        private String message;
        private InsightPriority priority;
        private boolean actionable;
        private String recommendation;

        public static StudyInsightBuilder builder() {
            return new StudyInsightBuilder();
        }

        // Getters
        public InsightType getType() { return type; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public InsightPriority getPriority() { return priority; }
        public boolean isActionable() { return actionable; }
        public String getRecommendation() { return recommendation; }

        public static class StudyInsightBuilder {
            private InsightType type;
            private String title;
            private String message;
            private InsightPriority priority;
            private boolean actionable;
            private String recommendation;

            public StudyInsightBuilder type(InsightType type) { this.type = type; return this; }
            public StudyInsightBuilder title(String title) { this.title = title; return this; }
            public StudyInsightBuilder message(String message) { this.message = message; return this; }
            public StudyInsightBuilder priority(InsightPriority priority) { this.priority = priority; return this; }
            public StudyInsightBuilder actionable(boolean actionable) { this.actionable = actionable; return this; }
            public StudyInsightBuilder recommendation(String recommendation) { this.recommendation = recommendation; return this; }

            public StudyInsight build() {
                StudyInsight insight = new StudyInsight();
                insight.type = this.type;
                insight.title = this.title;
                insight.message = this.message;
                insight.priority = this.priority;
                insight.actionable = this.actionable;
                insight.recommendation = this.recommendation;
                return insight;
            }
        }
    }

    public enum InsightType {
        PERFORMANCE, STREAK, ACTIVITY, PROGRESS, EFFICIENCY, ACHIEVEMENT
    }

    public enum InsightPriority {
        HIGH, MEDIUM, LOW, POSITIVE, NEUTRAL
    }
}
