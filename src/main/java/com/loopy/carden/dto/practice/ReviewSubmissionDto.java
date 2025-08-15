package com.loopy.carden.dto.practice;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for submitting card reviews
 */
@Data
public class ReviewSubmissionDto {

    @NotNull(message = "Grade is required")
    @Min(value = 0, message = "Grade must be between 0 and 3")
    @Max(value = 3, message = "Grade must be between 0 and 3")
    private Integer grade;

    @Min(value = 0, message = "Response time cannot be negative")
    private Integer responseTimeMs = 0;

    private String userAnswer; // For type-answer mode

    private Boolean showAnswer = false; // Whether user revealed answer early
}
