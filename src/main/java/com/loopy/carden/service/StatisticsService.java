package com.loopy.carden.service;

import com.loopy.carden.dto.statistics.PerformanceStatsDto;
import com.loopy.carden.dto.statistics.StudyStreakDto;
import com.loopy.carden.dto.statistics.UserStatisticsDto;
import com.loopy.carden.entity.ReviewSession;
import com.loopy.carden.entity.StudyState;
import com.loopy.carden.repository.ReviewSessionRepository;
import com.loopy.carden.repository.StudyStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for comprehensive statistics and performance tracking
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {

    private final StudyStateRepository studyStateRepository;
    private final ReviewSessionRepository reviewSessionRepository;

    /**
     * Get comprehensive user statistics
     */
    public UserStatisticsDto getUserStatistics(Long userId) {
        // Basic counts
        long totalCards = studyStateRepository.countByUserId(userId);
        long totalSessions = reviewSessionRepository.countByUserId(userId);
        long completedSessions = reviewSessionRepository.countCompletedSessionsByUserId(userId);
        long totalStudyTimeMinutes = reviewSessionRepository.calculateTotalStudyTimeByUserId(userId);
        long totalCardsStudied = reviewSessionRepository.calculateTotalCardsStudiedByUserId(userId);
        
        // Accuracy and performance
        Double overallAccuracy = reviewSessionRepository.calculateAverageAccuracyByUserId(userId);
        if (overallAccuracy == null) overallAccuracy = 0.0;
        
        // Study streak
        int currentStreak = reviewSessionRepository.calculateStudyStreak(userId);
        
        // Card state distribution
        List<Object[]> cardStateData = studyStateRepository.countCardsByState(userId);
        Map<String, Long> cardStateDistribution = cardStateData.stream()
                .collect(Collectors.toMap(
                    arr -> arr[0].toString(),
                    arr -> (Long) arr[1]
                ));
        
        // Recent activity (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<ReviewSession> recentSessions = reviewSessionRepository.findRecentSessionsByUser(userId, thirtyDaysAgo);
        
        int recentSessionCount = recentSessions.size();
        long recentStudyTime = recentSessions.stream()
                .mapToLong(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0)
                .sum();
        
        return UserStatisticsDto.builder()
                .userId(userId)
                .totalCards(totalCards)
                .totalSessions(totalSessions)
                .completedSessions(completedSessions)
                .totalStudyTimeMinutes(totalStudyTimeMinutes)
                .totalCardsStudied(totalCardsStudied)
                .overallAccuracy(overallAccuracy)
                .currentStreak(currentStreak)
                .cardStateDistribution(cardStateDistribution)
                .recentSessionCount(recentSessionCount)
                .recentStudyTimeMinutes(recentStudyTime)
                .lastActivityDate(getLastActivityDate(userId))
                .averageSessionDuration(calculateAverageSessionDuration(userId))
                .studyEfficiency(calculateStudyEfficiency(userId))
                .build();
    }

    /**
     * Get performance statistics over time
     */
    public PerformanceStatsDto getPerformanceStats(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<ReviewSession> sessions = reviewSessionRepository.findByUserIdAndSessionDateBetween(userId, startDate, endDate);
        
        if (sessions.isEmpty()) {
            return PerformanceStatsDto.builder()
                    .userId(userId)
                    .startDate(startDate)
                    .endDate(endDate)
                    .totalSessions(0)
                    .averageAccuracy(0.0)
                    .totalStudyTime(0L)
                    .cardsStudied(0)
                    .dailyAverages(Collections.emptyMap())
                    .weeklyTrends(Collections.emptyList())
                    .accuracyTrend(Collections.emptyList())
                    .build();
        }
        
        // Calculate basic metrics
        int totalSessions = sessions.size();
        double averageAccuracy = sessions.stream()
                .mapToDouble(s -> s.getAccuracyRate() != null ? s.getAccuracyRate() : 0.0)
                .average()
                .orElse(0.0);
        long totalStudyTime = sessions.stream()
                .mapToLong(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0)
                .sum();
        int cardsStudied = sessions.stream()
                .mapToInt(s -> s.getCardsStudied() != null ? s.getCardsStudied() : 0)
                .sum();
        
        // Daily averages
        Map<LocalDate, DailyStats> dailyStats = calculateDailyStats(sessions);
        Map<String, Double> dailyAverages = calculateDailyAverages(dailyStats);
        
        // Weekly trends
        List<WeeklyTrend> weeklyTrends = calculateWeeklyTrends(sessions);
        
        // Accuracy trend over time
        List<AccuracyPoint> accuracyTrend = calculateAccuracyTrend(sessions);
        
        return PerformanceStatsDto.builder()
                .userId(userId)
                .startDate(startDate)
                .endDate(endDate)
                .totalSessions(totalSessions)
                .averageAccuracy(averageAccuracy)
                .totalStudyTime(totalStudyTime)
                .cardsStudied(cardsStudied)
                .dailyAverages(dailyAverages)
                .weeklyTrends(weeklyTrends)
                .accuracyTrend(accuracyTrend)
                .improvementRate(calculateImprovementRate(accuracyTrend))
                .consistency(calculateConsistency(dailyStats))
                .build();
    }

    /**
     * Get study streak information
     */
    public StudyStreakDto getStudyStreak(Long userId) {
        int currentStreak = reviewSessionRepository.calculateStudyStreak(userId);
        
        // Calculate longest streak by finding all study dates and checking consecutive days
        List<ReviewSession> allSessions = reviewSessionRepository.findByUserIdOrderBySessionDateDesc(userId, null).getContent();
        
        Set<LocalDate> studyDates = allSessions.stream()
                .map(s -> s.getSessionDate().toLocalDate())
                .collect(Collectors.toSet());
        
        int longestStreak = calculateLongestStreak(studyDates);
        
        // Streak milestones
        List<Integer> milestones = Arrays.asList(7, 14, 30, 60, 100, 365);
        int nextMilestone = milestones.stream()
                .filter(m -> m > currentStreak)
                .findFirst()
                .orElse(currentStreak + 100); // Next 100-day milestone
        
        return StudyStreakDto.builder()
                .userId(userId)
                .currentStreak(currentStreak)
                .longestStreak(longestStreak)
                .nextMilestone(nextMilestone)
                .daysToMilestone(nextMilestone - currentStreak)
                .studyDates(studyDates)
                .streakStartDate(calculateStreakStartDate(userId, currentStreak))
                .isActive(isStreakActive(userId))
                .build();
    }

    /**
     * Get deck-specific statistics
     */
    public Map<String, Object> getDeckStatistics(Long userId, Long deckId) {
        List<StudyState> deckStudyStates = studyStateRepository.findByDeckId(deckId);
        List<StudyState> userDeckStates = deckStudyStates.stream()
                .filter(s -> s.getUser().getId().equals(userId))
                .collect(Collectors.toList());
        
        if (userDeckStates.isEmpty()) {
            return Map.of(
                "totalCards", 0,
                "studiedCards", 0,
                "averageAccuracy", 0.0,
                "masteredCards", 0,
                "difficultCards", 0
            );
        }
        
        int totalCards = userDeckStates.size();
        int studiedCards = (int) userDeckStates.stream()
                .filter(s -> s.getTotalReviews() > 0)
                .count();
        
        double averageAccuracy = userDeckStates.stream()
                .filter(s -> s.getTotalReviews() > 0)
                .mapToDouble(StudyState::getAccuracyRate)
                .average()
                .orElse(0.0);
        
        int masteredCards = (int) userDeckStates.stream()
                .filter(s -> s.getAccuracyRate() >= 90.0 && s.getTotalReviews() >= 5)
                .count();
        
        int difficultCards = (int) userDeckStates.stream()
                .filter(s -> s.getAccuracyRate() < 50.0 && s.getTotalReviews() >= 3)
                .count();
        
        return Map.of(
            "totalCards", totalCards,
            "studiedCards", studiedCards,
            "averageAccuracy", averageAccuracy,
            "masteredCards", masteredCards,
            "difficultCards", difficultCards,
            "completionRate", (double) studiedCards / totalCards * 100.0
        );
    }

    /**
     * Get leech cards (cards that are frequently failed)
     */
    public List<Map<String, Object>> getLeechCards(Long userId, int minFailures) {
        // This would require additional tracking in StudyState entity
        // For now, return cards with low accuracy and many reviews
        List<StudyState> potentialLeeches = studyStateRepository.findAll().stream()
                .filter(s -> s.getUser().getId().equals(userId))
                .filter(s -> s.getAccuracyRate() < 40.0 && s.getTotalReviews() >= minFailures)
                .sorted((a, b) -> Double.compare(a.getAccuracyRate(), b.getAccuracyRate()))
                .collect(Collectors.toList());
        
        return potentialLeeches.stream()
                .map(s -> {
                    Map<String, Object> leechInfo = new HashMap<>();
                    leechInfo.put("cardId", s.getCard().getId());
                    leechInfo.put("accuracyRate", s.getAccuracyRate());
                    leechInfo.put("totalReviews", s.getTotalReviews());
                    leechInfo.put("consecutiveFailures", s.getConsecutiveFailures() != null ? s.getConsecutiveFailures() : 0);
                    leechInfo.put("lastReviewDate", s.getLastReviewDate());
                    return leechInfo;
                })
                .collect(Collectors.toList());
    }

    // Helper methods for calculations
    private LocalDateTime getLastActivityDate(Long userId) {
        return reviewSessionRepository.findByUserIdOrderBySessionDateDesc(userId, null)
                .stream()
                .findFirst()
                .map(ReviewSession::getSessionDate)
                .orElse(null);
    }

    private double calculateAverageSessionDuration(Long userId) {
        List<ReviewSession> completedSessions = reviewSessionRepository.findByUserIdAndSessionStatus(userId, 
                ReviewSession.SessionStatus.COMPLETED);
        
        return completedSessions.stream()
                .filter(s -> s.getDurationMinutes() != null)
                .mapToInt(ReviewSession::getDurationMinutes)
                .average()
                .orElse(0.0);
    }

    private double calculateStudyEfficiency(Long userId) {
        // Efficiency = Cards studied per minute
        long totalStudyTime = reviewSessionRepository.calculateTotalStudyTimeByUserId(userId);
        long totalCards = reviewSessionRepository.calculateTotalCardsStudiedByUserId(userId);
        
        if (totalStudyTime == 0) return 0.0;
        return (double) totalCards / totalStudyTime;
    }

    private Map<LocalDate, DailyStats> calculateDailyStats(List<ReviewSession> sessions) {
        return sessions.stream()
                .collect(Collectors.groupingBy(
                    s -> s.getSessionDate().toLocalDate(),
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        daySessionList -> {
                            int sessionCount = daySessionList.size();
                            long studyTime = daySessionList.stream()
                                    .mapToLong(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0)
                                    .sum();
                            int cardsStudied = daySessionList.stream()
                                    .mapToInt(s -> s.getCardsStudied() != null ? s.getCardsStudied() : 0)
                                    .sum();
                            double avgAccuracy = daySessionList.stream()
                                    .mapToDouble(s -> s.getAccuracyRate() != null ? s.getAccuracyRate() : 0.0)
                                    .average()
                                    .orElse(0.0);
                            
                            return new DailyStats(sessionCount, studyTime, cardsStudied, avgAccuracy);
                        }
                    )
                ));
    }

    private Map<String, Double> calculateDailyAverages(Map<LocalDate, DailyStats> dailyStats) {
        if (dailyStats.isEmpty()) {
            return Map.of(
                "averageSessions", 0.0,
                "averageStudyTime", 0.0,
                "averageCards", 0.0,
                "averageAccuracy", 0.0
            );
        }
        
        double avgSessions = dailyStats.values().stream()
                .mapToInt(DailyStats::getSessionCount)
                .average()
                .orElse(0.0);
        double avgStudyTime = dailyStats.values().stream()
                .mapToLong(DailyStats::getStudyTime)
                .average()
                .orElse(0.0);
        double avgCards = dailyStats.values().stream()
                .mapToInt(DailyStats::getCardsStudied)
                .average()
                .orElse(0.0);
        double avgAccuracy = dailyStats.values().stream()
                .mapToDouble(DailyStats::getAvgAccuracy)
                .average()
                .orElse(0.0);
        
        return Map.of(
            "averageSessions", avgSessions,
            "averageStudyTime", avgStudyTime,
            "averageCards", avgCards,
            "averageAccuracy", avgAccuracy
        );
    }

    private List<WeeklyTrend> calculateWeeklyTrends(List<ReviewSession> sessions) {
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        
        return sessions.stream()
                .collect(Collectors.groupingBy(
                    s -> Map.entry(
                        s.getSessionDate().get(weekFields.weekOfWeekBasedYear()),
                        s.getSessionDate().getYear()
                    )
                ))
                .entrySet().stream()
                .map(entry -> {
                    Map.Entry<Integer, Integer> weekYear = entry.getKey();
                    List<ReviewSession> weekSessions = entry.getValue();
                    
                    int totalSessions = weekSessions.size();
                    long totalTime = weekSessions.stream()
                            .mapToLong(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 0)
                            .sum();
                    int totalCards = weekSessions.stream()
                            .mapToInt(s -> s.getCardsStudied() != null ? s.getCardsStudied() : 0)
                            .sum();
                    double avgAccuracy = weekSessions.stream()
                            .mapToDouble(s -> s.getAccuracyRate() != null ? s.getAccuracyRate() : 0.0)
                            .average()
                            .orElse(0.0);
                    
                    return new WeeklyTrend(weekYear.getValue(), weekYear.getKey(), 
                            totalSessions, totalTime, totalCards, avgAccuracy);
                })
                .sorted(Comparator.comparing((WeeklyTrend wt) -> wt.getYear()).thenComparing(WeeklyTrend::getWeek))
                .collect(Collectors.toList());
    }

    private List<AccuracyPoint> calculateAccuracyTrend(List<ReviewSession> sessions) {
        return sessions.stream()
                .sorted(Comparator.comparing(ReviewSession::getSessionDate))
                .map(s -> new AccuracyPoint(s.getSessionDate(), 
                        s.getAccuracyRate() != null ? s.getAccuracyRate() : 0.0))
                .collect(Collectors.toList());
    }

    private double calculateImprovementRate(List<AccuracyPoint> accuracyTrend) {
        if (accuracyTrend.size() < 2) return 0.0;
        
        AccuracyPoint first = accuracyTrend.get(0);
        AccuracyPoint last = accuracyTrend.get(accuracyTrend.size() - 1);
        
        long daysBetween = ChronoUnit.DAYS.between(first.getDate(), last.getDate());
        if (daysBetween == 0) return 0.0;
        
        return (last.getAccuracy() - first.getAccuracy()) / daysBetween;
    }

    private double calculateConsistency(Map<LocalDate, DailyStats> dailyStats) {
        if (dailyStats.size() < 2) return 0.0;
        
        List<Double> accuracies = dailyStats.values().stream()
                .map(DailyStats::getAvgAccuracy)
                .collect(Collectors.toList());
        
        double mean = accuracies.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = accuracies.stream()
                .mapToDouble(acc -> Math.pow(acc - mean, 2))
                .average()
                .orElse(0.0);
        
        double standardDeviation = Math.sqrt(variance);
        return Math.max(0.0, 100.0 - standardDeviation); // Higher consistency = lower deviation
    }

    private int calculateLongestStreak(Set<LocalDate> studyDates) {
        if (studyDates.isEmpty()) return 0;
        
        List<LocalDate> sortedDates = studyDates.stream()
                .sorted()
                .collect(Collectors.toList());
        
        int longestStreak = 1;
        int currentStreak = 1;
        
        for (int i = 1; i < sortedDates.size(); i++) {
            LocalDate current = sortedDates.get(i);
            LocalDate previous = sortedDates.get(i - 1);
            
            if (ChronoUnit.DAYS.between(previous, current) == 1) {
                currentStreak++;
                longestStreak = Math.max(longestStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }
        
        return longestStreak;
    }

    private LocalDate calculateStreakStartDate(Long userId, int currentStreak) {
        if (currentStreak == 0) return null;
        return LocalDate.now().minusDays(currentStreak - 1);
    }

    private boolean isStreakActive(Long userId) {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1).withHour(0).withMinute(0);
        LocalDateTime today = LocalDateTime.now().withHour(23).withMinute(59);
        
        List<ReviewSession> recentSessions = reviewSessionRepository.findByUserIdAndSessionDateBetween(
                userId, yesterday, today);
        
        return !recentSessions.isEmpty();
    }

    // Helper classes for data aggregation
    public static class DailyStats {
        private final int sessionCount;
        private final long studyTime;
        private final int cardsStudied;
        private final double avgAccuracy;

        public DailyStats(int sessionCount, long studyTime, int cardsStudied, double avgAccuracy) {
            this.sessionCount = sessionCount;
            this.studyTime = studyTime;
            this.cardsStudied = cardsStudied;
            this.avgAccuracy = avgAccuracy;
        }

        public int getSessionCount() { return sessionCount; }
        public long getStudyTime() { return studyTime; }
        public int getCardsStudied() { return cardsStudied; }
        public double getAvgAccuracy() { return avgAccuracy; }
    }

    public static class WeeklyTrend {
        private final int year;
        private final int week;
        private final int sessions;
        private final long studyTime;
        private final int cardsStudied;
        private final double avgAccuracy;

        public WeeklyTrend(int year, int week, int sessions, long studyTime, int cardsStudied, double avgAccuracy) {
            this.year = year;
            this.week = week;
            this.sessions = sessions;
            this.studyTime = studyTime;
            this.cardsStudied = cardsStudied;
            this.avgAccuracy = avgAccuracy;
        }

        public int getYear() { return year; }
        public int getWeek() { return week; }
        public int getSessions() { return sessions; }
        public long getStudyTime() { return studyTime; }
        public int getCardsStudied() { return cardsStudied; }
        public double getAvgAccuracy() { return avgAccuracy; }
    }

    public static class AccuracyPoint {
        private final LocalDateTime date;
        private final double accuracy;

        public AccuracyPoint(LocalDateTime date, double accuracy) {
            this.date = date;
            this.accuracy = accuracy;
        }

        public LocalDateTime getDate() { return date; }
        public double getAccuracy() { return accuracy; }
    }
}
