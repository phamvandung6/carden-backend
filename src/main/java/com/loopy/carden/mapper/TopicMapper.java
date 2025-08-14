package com.loopy.carden.mapper;

import com.loopy.carden.dto.topic.TopicResponseDto;
import com.loopy.carden.entity.Topic;

import java.util.List;

public final class TopicMapper {

    private TopicMapper() {}

    public static TopicResponseDto toResponseDto(Topic topic) {
        return TopicResponseDto.builder()
                .id(topic.getId())
                .name(topic.getName())
                .description(topic.getDescription())
                .isSystemTopic(topic.isSystemTopic())
                .displayOrder(topic.getDisplayOrder())
                .parentTopicId(topic.getParentTopic() != null ? topic.getParentTopic().getId() : null)
                .createdAt(topic.getCreatedAt())
                .updatedAt(topic.getUpdatedAt())
                .build();
    }

    public static List<TopicResponseDto> toResponseDtoList(List<Topic> topics) {
        return topics.stream()
                .map(TopicMapper::toResponseDto)
                .toList();
    }
}
