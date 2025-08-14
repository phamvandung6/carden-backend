package com.loopy.carden.dto.topic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicResponseDto {
    private Long id;
    private String name;
    private String description;
    private boolean isSystemTopic;
    private Integer displayOrder;
    private Long parentTopicId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
