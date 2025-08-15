package com.loopy.carden.dto.practice;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for due cards count information
 */
@Data
@Builder
public class DueCardsCountDto {
    private Integer totalDue;
    private Integer newCards;
    private Integer reviewCards;
    private Integer learningCards;
}
