package com.loopy.carden.dto.practice;

import com.loopy.carden.dto.studystate.StudyStateResponseDto;
import lombok.Data;

/**
 * DTO for review submission results
 */
@Data
public class ReviewResultDto {
    
    private Boolean success;
    private String message;
    
    // Updated study state after review
    private StudyStateResponseDto.SimplifiedDto updatedStudyState;
    
    // Next card to practice (if available)
    private PracticeCardDto nextCard;
    
    // Session progress
    private SessionProgressDto sessionProgress;
    
    @Data
    public static class SessionProgressDto {
        private Integer cardsStudied;
        private Integer cardsCorrect;
        private Double currentAccuracy;
        private Integer remainingCards;
        private Integer sessionDurationMinutes;
    }
}
