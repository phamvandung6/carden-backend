package com.loopy.carden.dto.practice;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DTO for type-answer study mode cards
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TypeAnswerCardDto extends PracticeCardDto {
    
    private String placeholder; // Hint for what to type
    private boolean showHint = true; // Whether to show typing hints
    private int maxLength; // Maximum expected answer length
    private boolean caseSensitive = false; // Whether answer is case sensitive
    
    public TypeAnswerCardDto() {
        super();
    }
}

