package com.loopy.carden.dto.topic;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicUpdateDto {
    
    @Size(max = 100, message = "Topic name cannot exceed 100 characters")
    private String name;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    
    private Boolean isSystemTopic;
    
    private Integer displayOrder;
    
    private Long parentTopicId;
}
