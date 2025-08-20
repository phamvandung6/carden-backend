package com.loopy.carden.dto.card;

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
public class BulkCardGenerationResponseDto {

    private Boolean success;
    
    private String message;
    
    private Integer totalRequested;
    
    private Integer totalGenerated;
    
    private Integer totalSaved;
    
    private Long deckId;
    
    private List<String> errors;
    
    private List<String> warnings;
    
    @Builder.Default
    private LocalDateTime processedAt = LocalDateTime.now();
    
    private Long processingTimeMs;
    
    // Summary information about the generation
    private GenerationSummary summary;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerationSummary {
        private String topic;
        private String sourceLanguage;
        private String targetLanguage;
        private String cefrLevel;
        private Integer duplicatesSkipped;
        private Integer validationErrors;
    }
}
