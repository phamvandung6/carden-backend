package com.loopy.carden.dto.practice;

import com.loopy.carden.dto.card.CardResponseDto;
import com.loopy.carden.dto.studystate.StudyStateResponseDto;
import com.loopy.carden.entity.StudyState.CardState;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for practice cards with study state information
 */
@Data
public class PracticeCardDto {
    
    // Card information
    private Long cardId;
    private String frontText;
    private String frontImageUrl;
    private String backDefinition;
    private String backMeaningVi;
    private String ipa;
    
    // Study state information
    private Long studyStateId;
    private CardState cardState;
    private LocalDateTime dueDate;
    private Integer intervalDays;
    private Integer totalReviews;
    private Double accuracyRate;
    private Boolean isDue;
    private Boolean isNew;
    private Boolean isLearning;
    
    // Deck information
    private Long deckId;
    private String deckTitle;
    
    // Practice metadata
    private Boolean showAnswer = false;
    private Integer remainingNewCards;
    private Integer remainingReviewCards;
    private Integer remainingLearningCards;
}
