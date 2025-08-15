package com.loopy.carden.dto.practice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for submitting type-answer mode responses
 */
@Data
public class TypeAnswerSubmissionDto {
    
    @NotNull(message = "Card ID is required")
    private Long cardId;
    
    @NotBlank(message = "User answer is required")
    private String userAnswer;
    
    private Integer responseTimeMs = 0; // Time taken to answer
    
    private Boolean showAnswer = false; // Whether user revealed answer early
}

