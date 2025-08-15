package com.loopy.carden.dto.practice;

import com.loopy.carden.dto.studystate.StudyStateResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for review submission results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResultDto {
    
    private Long cardId;
    private Integer grade;
    private Boolean isCorrect;
    private String feedback;
    private Double similarity; // For type-answer mode
    private String correctAnswer;
    private String userAnswer;
    private LocalDateTime nextReviewDate;
    private String currentState;
    private String message;
    private Boolean success;
    
    // Updated study state after review
    private StudyStateResponseDto.SimplifiedDto updatedStudyState;
    
    // Next card to practice (if available)
    private PracticeCardDto nextCard;
    
    // Session progress
    private SessionProgressDto sessionProgress;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionProgressDto {
        private Integer cardsStudied;
        private Integer cardsCorrect;
        private Double currentAccuracy;
        private Integer remainingCards;
        private Integer sessionDurationMinutes;
    }
}
