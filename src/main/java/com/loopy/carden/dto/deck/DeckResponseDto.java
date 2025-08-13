package com.loopy.carden.dto.deck;

import com.loopy.carden.entity.Deck;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
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
    private boolean publicDeck;
    private boolean systemDeck;
    private Long downloadCount;
    private Long likeCount;
    private Integer cardCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


