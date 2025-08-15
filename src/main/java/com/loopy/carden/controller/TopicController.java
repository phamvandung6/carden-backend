package com.loopy.carden.controller;

import com.loopy.carden.dto.StandardResponse;
import com.loopy.carden.dto.topic.TopicCreateDto;
import com.loopy.carden.dto.topic.TopicResponseDto;
import com.loopy.carden.dto.topic.TopicUpdateDto;
import com.loopy.carden.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/topics")
@RequiredArgsConstructor
@Tag(name = "Topics", description = "Topic management operations")
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    @Operation(summary = "Get all topics", description = "Public endpoint to retrieve all available topics")
    public ResponseEntity<StandardResponse<List<TopicResponseDto>>> getAllTopics() {
        var topics = topicService.findAll();
        return ResponseEntity.ok(StandardResponse.success(topics));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get topic by ID", description = "Public endpoint to retrieve a specific topic by ID")
    public ResponseEntity<StandardResponse<TopicResponseDto>> getTopicById(
            @Parameter(description = "Topic ID") @PathVariable Long id) {
        var topic = topicService.findById(id);
        return ResponseEntity.ok(StandardResponse.success(topic));
    }

    @GetMapping("/root")
    @Operation(summary = "Get root topics", description = "Public endpoint to retrieve only root-level topics")
    public ResponseEntity<StandardResponse<List<TopicResponseDto>>> getRootTopics() {
        var topics = topicService.findRootTopics();
        return ResponseEntity.ok(StandardResponse.success(topics));
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Get child topics", description = "Public endpoint to retrieve child topics of a specific topic")
    public ResponseEntity<StandardResponse<List<TopicResponseDto>>> getChildTopics(
            @Parameter(description = "Parent topic ID") @PathVariable Long id) {
        var topics = topicService.findChildTopics(id);
        return ResponseEntity.ok(StandardResponse.success(topics));
    }

    @PostMapping
    @Operation(
        summary = "Create new topic", 
        description = "Admin-only endpoint to create a new topic",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StandardResponse<TopicResponseDto>> createTopic(
            @Valid @RequestBody TopicCreateDto createDto) {
        var topic = topicService.create(createDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.success(topic));
    }

    @PatchMapping("/{id}")
    @Operation(
        summary = "Update topic", 
        description = "Admin-only endpoint to update an existing topic",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StandardResponse<TopicResponseDto>> updateTopic(
            @Parameter(description = "Topic ID") @PathVariable Long id,
            @Valid @RequestBody TopicUpdateDto updateDto) {
        var topic = topicService.update(id, updateDto);
        return ResponseEntity.ok(StandardResponse.success(topic));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete topic", 
        description = "Admin-only endpoint to delete a topic (only if it has no children or decks)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StandardResponse<String>> deleteTopic(
            @Parameter(description = "Topic ID") @PathVariable Long id) {
        topicService.delete(id);
        return ResponseEntity.ok(StandardResponse.success("Topic deleted successfully"));
    }
}
