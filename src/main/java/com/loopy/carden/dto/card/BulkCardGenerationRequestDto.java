package com.loopy.carden.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkCardGenerationRequestDto {

    // Deck ID is set from path parameter in controller
    @Schema(hidden = true)
    private Long deckId;

    @NotBlank(message = "Topic is required")
    @Size(max = 200, message = "Topic must not exceed 200 characters")
    private String topic;

    @Min(value = 1, message = "Count must be at least 1")
    @Max(value = 15, message = "Cannot generate more than 15 cards at once")
    @Builder.Default
    private Integer count = 10;

    // Optional advanced controls (kept hidden in Swagger to simplify request)
    @Schema(hidden = true)
    @Size(max = 20, message = "Maximum 20 keywords allowed")
    private List<@NotBlank @Size(max = 100, message = "Keyword must not exceed 100 characters") String> keywords;

    @Schema(hidden = true)
    @Builder.Default
    private Boolean includeExamples = true;

    @Schema(hidden = true)
    @Builder.Default
    private Boolean includePronunciation = true;

    @Schema(hidden = true)
    @Size(max = 1000, message = "Additional context must not exceed 1000 characters")
    private String additionalContext;
}
