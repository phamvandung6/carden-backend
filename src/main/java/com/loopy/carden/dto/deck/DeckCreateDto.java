package com.loopy.carden.dto.deck;

import com.loopy.carden.entity.Deck;
import jakarta.validation.constraints.NotBlank;
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
public class DeckCreateDto {

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 1000)
    private String description;

    private Long topicId;

    private Deck.CEFRLevel cefrLevel;

    @Size(max = 10)
    private String sourceLanguage;

    @Size(max = 10)
    private String targetLanguage;

    private List<String> tags;

    private String coverImageUrl;
}


