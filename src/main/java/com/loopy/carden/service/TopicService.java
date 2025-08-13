package com.loopy.carden.service;

import com.loopy.carden.entity.Topic;
import com.loopy.carden.exception.ResourceNotFoundException;
import com.loopy.carden.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;

    public Topic getByIdOrThrow(Long id) {
        if (id == null) {
            return null;
        }
        return topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + id));
    }

    public List<Topic> findAll() {
        return topicRepository.findAll();
    }
}


