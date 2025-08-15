package com.loopy.carden.dto.practice;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for submitting multiple choice mode responses
 */
@Data
public class MultipleChoiceSubmissionDto {
    
    @NotNull(message = "Card ID is required")
    private Long cardId;
    
    @NotNull(message = "Selected option is required")
    @Min(value = 0, message = "Selected option must be between 0 and 3")
    @Max(value = 3, message = "Selected option must be between 0 and 3")
    private Integer selectedOption; // Index of selected option (0-3)
    
    private Integer responseTimeMs = 0; // Time taken to answer
    
    private Boolean showAnswer = false; // Whether user revealed answer early
}

