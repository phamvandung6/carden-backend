package com.loopy.carden.dto.practice;

import com.loopy.carden.entity.ReviewSession.StudyMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for starting practice sessions
 */
@Data
public class PracticeSessionStartDto {

    @NotNull(message = "Study mode is required")
    private StudyMode studyMode;

    private Long deckId; // Optional - null for mixed deck sessions

    private Integer maxNewCards = 20; // Limit new cards per session

    private Integer maxReviewCards = 200; // Limit review cards per session

    private Boolean includeNewCards = true;

    private Boolean includeReviewCards = true;

    private Boolean includeLearningCards = true;
}
