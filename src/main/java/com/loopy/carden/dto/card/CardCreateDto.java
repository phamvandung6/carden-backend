package com.loopy.carden.dto.card;

import com.loopy.carden.entity.Card;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class CardCreateDto {

    @NotBlank(message = "Front text is required")
    @Size(max = 500, message = "Front text must not exceed 500 characters")
    private String front;

    @NotBlank(message = "Back text is required")
    @Size(max = 500, message = "Back text must not exceed 500 characters")
    private String back;

    @Size(max = 200, message = "IPA pronunciation must not exceed 200 characters")
    @Pattern(regexp = "^[\\p{L}\\p{M}\\p{N}\\p{P}\\p{S}\\s]*$", message = "IPA pronunciation contains invalid characters")
    private String ipaPronunciation;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    @Pattern(regexp = "^(https?://.*\\.(jpg|jpeg|png|gif|webp))(\\?.*)?$", 
             message = "Image URL must be a valid HTTP/HTTPS URL pointing to an image file")
    private String imageUrl;

    @Size(max = 500, message = "Audio URL must not exceed 500 characters")
    @Pattern(regexp = "^(https?://.*\\.(mp3|wav|ogg|m4a))(\\?.*)?$", 
             message = "Audio URL must be a valid HTTP/HTTPS URL pointing to an audio file")
    private String audioUrl;

    @Size(max = 10, message = "Maximum 10 examples allowed")
    private List<@NotBlank @Size(max = 200, message = "Example must not exceed 200 characters") String> examples;

    @Size(max = 10, message = "Maximum 10 synonyms allowed")
    private List<@NotBlank @Size(max = 100, message = "Synonym must not exceed 100 characters") String> synonyms;

    @Size(max = 10, message = "Maximum 10 antonyms allowed")
    private List<@NotBlank @Size(max = 100, message = "Antonym must not exceed 100 characters") String> antonyms;

    @Size(max = 10, message = "Maximum 10 tags allowed")
    private List<@NotBlank @Size(max = 50, message = "Tag must not exceed 50 characters") String> tags;

    @Builder.Default
    private Card.Difficulty difficulty = Card.Difficulty.NORMAL;

    private Integer displayOrder;
}
