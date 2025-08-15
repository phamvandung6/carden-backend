package com.loopy.carden.service;

import com.loopy.carden.dto.topic.TopicCreateDto;
import com.loopy.carden.dto.topic.TopicResponseDto;
import com.loopy.carden.dto.topic.TopicUpdateDto;
import com.loopy.carden.entity.Topic;
import com.loopy.carden.exception.BadRequestException;
import com.loopy.carden.exception.ResourceNotFoundException;
import com.loopy.carden.mapper.TopicMapper;
import com.loopy.carden.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TopicService {

    private final TopicRepository topicRepository;

    public Topic getByIdOrThrow(Long id) {
        if (id == null) {
            return null;
        }
        return topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + id));
    }

    public List<TopicResponseDto> findAll() {
        return TopicMapper.toResponseDtoList(topicRepository.findAll());
    }

    public TopicResponseDto findById(Long id) {
        Topic topic = getByIdOrThrow(id);
        return TopicMapper.toResponseDto(topic);
    }

    public TopicResponseDto create(TopicCreateDto createDto) {
        // Validate parent topic if provided
        if (createDto.getParentTopicId() != null) {
            Topic parentTopic = getByIdOrThrow(createDto.getParentTopicId());
            if (parentTopic.isSystemTopic()) {
                throw new BadRequestException("Cannot create child topics under system topics");
            }
        }

        Topic topic = TopicMapper.toEntity(createDto);
        
        // Set parent topic if provided
        if (createDto.getParentTopicId() != null) {
            Topic parentTopic = getByIdOrThrow(createDto.getParentTopicId());
            topic.setParentTopic(parentTopic);
        }

        Topic savedTopic = topicRepository.save(topic);
        return TopicMapper.toResponseDto(savedTopic);
    }

    public TopicResponseDto update(Long id, TopicUpdateDto updateDto) {
        Topic topic = getByIdOrThrow(id);
        
        // Prevent updating system topics
        if (topic.isSystemTopic()) {
            throw new BadRequestException("Cannot modify system topics");
        }

        // Validate parent topic if changing
        if (updateDto.getParentTopicId() != null && 
            !updateDto.getParentTopicId().equals(topic.getParentTopic() != null ? topic.getParentTopic().getId() : null)) {
            
            Topic newParentTopic = getByIdOrThrow(updateDto.getParentTopicId());
            if (newParentTopic.isSystemTopic()) {
                throw new BadRequestException("Cannot move topics under system topics");
            }
            
            // Check for circular reference
            if (isCircularReference(id, updateDto.getParentTopicId())) {
                throw new BadRequestException("Circular reference detected in topic hierarchy");
            }
            
            topic.setParentTopic(newParentTopic);
        }

        TopicMapper.updateEntityFromDto(topic, updateDto);
        Topic updatedTopic = topicRepository.save(topic);
        return TopicMapper.toResponseDto(updatedTopic);
    }

    public void delete(Long id) {
        Topic topic = getByIdOrThrow(id);
        
        // Prevent deleting system topics
        if (topic.isSystemTopic()) {
            throw new BadRequestException("Cannot delete system topics");
        }

        // Check if topic has child topics
        if (topic.getChildTopics() != null && !topic.getChildTopics().isEmpty()) {
            throw new BadRequestException("Cannot delete topic with child topics. Please delete child topics first.");
        }

        // Check if topic has decks
        if (topic.getDecks() != null && !topic.getDecks().isEmpty()) {
            throw new BadRequestException("Cannot delete topic with decks. Please remove or reassign decks first.");
        }

        topicRepository.delete(topic);
    }

    private boolean isCircularReference(Long topicId, Long newParentId) {
        if (topicId.equals(newParentId)) {
            return true;
        }
        
        Topic current = topicRepository.findById(newParentId).orElse(null);
        while (current != null && current.getParentTopic() != null) {
            if (current.getParentTopic().getId().equals(topicId)) {
                return true;
            }
            current = current.getParentTopic();
        }
        
        return false;
    }

    public List<TopicResponseDto> findRootTopics() {
        List<Topic> rootTopics = topicRepository.findRootTopicsOrderByDisplayOrder();
        return TopicMapper.toResponseDtoList(rootTopics);
    }

    public List<TopicResponseDto> findChildTopics(Long parentId) {
        Topic parentTopic = getByIdOrThrow(parentId);
        List<Topic> childTopics = topicRepository.findChildTopicsOrderByDisplayOrder(parentTopic);
        return TopicMapper.toResponseDtoList(childTopics);
    }
}


