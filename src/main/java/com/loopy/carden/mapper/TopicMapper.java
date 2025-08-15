package com.loopy.carden.mapper;

import com.loopy.carden.dto.topic.TopicCreateDto;
import com.loopy.carden.dto.topic.TopicResponseDto;
import com.loopy.carden.dto.topic.TopicUpdateDto;
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

    public static Topic toEntity(TopicCreateDto dto) {
        Topic topic = new Topic();
        topic.setName(dto.getName());
        topic.setDescription(dto.getDescription());
        topic.setSystemTopic(dto.isSystemTopic());
        topic.setDisplayOrder(dto.getDisplayOrder());
        return topic;
    }

    public static void updateEntityFromDto(Topic topic, TopicUpdateDto dto) {
        if (dto.getName() != null) {
            topic.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            topic.setDescription(dto.getDescription());
        }
        if (dto.getIsSystemTopic() != null) {
            topic.setSystemTopic(dto.getIsSystemTopic());
        }
        if (dto.getDisplayOrder() != null) {
            topic.setDisplayOrder(dto.getDisplayOrder());
        }
    }
}
