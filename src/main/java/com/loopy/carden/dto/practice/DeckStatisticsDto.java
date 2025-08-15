package com.loopy.carden.dto.practice;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for deck-specific statistics
 */
@Data
@Builder
public class DeckStatisticsDto {
    private Long deckId;
    private Integer totalCards;
    private Integer studiedCards;
    private Integer masteredCards;
    private Double averageAccuracy;
    private Double completionRate;
}
