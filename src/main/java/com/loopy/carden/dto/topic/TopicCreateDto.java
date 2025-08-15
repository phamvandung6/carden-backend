package com.loopy.carden.dto.topic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicCreateDto {
    
    @NotBlank(message = "Topic name is required")
    @Size(max = 100, message = "Topic name cannot exceed 100 characters")
    private String name;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    
    private boolean isSystemTopic = false;
    
    private Integer displayOrder = 0;
    
    private Long parentTopicId;
}
