package com.loopy.carden.dto.card;

import com.loopy.carden.entity.Card;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardResponseDto {
    
    private Long id;
    private Long deckId;
    private String front;
    private String back;
    private String ipaPronunciation;
    private String imageUrl;
    private String audioUrl;
    private List<String> examples;
    private List<String> synonyms;
    private List<String> antonyms;
    private List<String> tags;
    private Card.Difficulty difficulty;
    private Integer displayOrder;
    private String uniqueKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Study statistics (if available)
    private StudyStats studyStats;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudyStats {
        private Integer totalReviews;
        private Integer correctReviews;
        private Double accuracyRate;
        private LocalDateTime lastReviewed;
        private String currentState; // NEW, LEARNING, REVIEW, RELEARNING
    }
}
