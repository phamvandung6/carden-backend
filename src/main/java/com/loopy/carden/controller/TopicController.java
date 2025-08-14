package com.loopy.carden.controller;

import com.loopy.carden.dto.StandardResponse;
import com.loopy.carden.dto.topic.TopicResponseDto;
import com.loopy.carden.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/topics")
@RequiredArgsConstructor
@Tag(name = "Topics", description = "Topic management operations")
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    @Operation(summary = "Get all topics")
    public ResponseEntity<StandardResponse<List<TopicResponseDto>>> getAllTopics() {
        var topics = topicService.findAll();
        return ResponseEntity.ok(StandardResponse.success(topics));
    }
}
