package com.loopy.carden.dto.statistics;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

/**
 * DTO for study streak information
 */
@Data
@Builder
public class StudyStreakDto {
    private Long userId;
    private Integer currentStreak;
    private Integer longestStreak;
    private Integer nextMilestone;
    private Integer daysToMilestone;
    private Set<LocalDate> studyDates;
    private LocalDate streakStartDate;
    private Boolean isActive;
}
