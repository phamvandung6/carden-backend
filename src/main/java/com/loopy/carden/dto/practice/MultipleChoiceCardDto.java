package com.loopy.carden.dto.practice;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * DTO for multiple choice study mode cards
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MultipleChoiceCardDto extends PracticeCardDto {
    
    private List<ChoiceOption> options; // 4 options with 1 correct
    private int correctOptionIndex; // Index of correct answer (0-3)
    private boolean shuffled = true; // Whether options are shuffled
    
    @Data
    public static class ChoiceOption {
        private String text;
        private boolean isCorrect;
        private String explanation; // Optional explanation for why it's right/wrong
        
        public ChoiceOption(String text, boolean isCorrect) {
            this.text = text;
            this.isCorrect = isCorrect;
        }
        
        public ChoiceOption(String text, boolean isCorrect, String explanation) {
            this.text = text;
            this.isCorrect = isCorrect;
            this.explanation = explanation;
        }
    }
    
    public MultipleChoiceCardDto() {
        super();
    }
}

