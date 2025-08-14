package com.loopy.carden.dto.deck;

import com.loopy.carden.entity.Deck;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeckUpdateDto {
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private Long topicId;

    private Deck.Visibility visibility;

    private Deck.CEFRLevel cefrLevel;

    @Size(max = 10, message = "Source language must not exceed 10 characters")
    private String sourceLanguage;

    @Size(max = 10, message = "Target language must not exceed 10 characters")
    private String targetLanguage;

    private String coverImageUrl;

    @Size(max = 10, message = "A deck can have at most 10 tags")
    private List<String> tags;
}


