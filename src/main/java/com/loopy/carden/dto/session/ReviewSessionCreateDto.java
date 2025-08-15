package com.loopy.carden.dto.session;

import com.loopy.carden.entity.ReviewSession.StudyMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for creating review sessions
 */
@Data
public class ReviewSessionCreateDto {

    @NotNull(message = "Study mode is required")
    private StudyMode studyMode;

    private Long deckId; // Optional - null for mixed deck sessions
}
