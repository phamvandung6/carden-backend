package com.loopy.carden.dto.deck;

import com.loopy.carden.entity.Deck;
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
public class DeckResponseDto {
    private Long id;
    private String title;
    private String description;
    private Long userId;
    private Long topicId;
    private Deck.Visibility visibility;
    private Deck.CEFRLevel cefrLevel;
    private String sourceLanguage;
    private String targetLanguage;
    private String coverImageUrl;
    private List<String> tags;

    private boolean systemDeck;
    private Long downloadCount;
    private Long likeCount;
    private Integer cardCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


