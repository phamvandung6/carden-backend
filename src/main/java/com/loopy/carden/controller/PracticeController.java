package com.loopy.carden.controller;

import com.loopy.carden.dto.StandardResponse;
import com.loopy.carden.dto.practice.*;

import com.loopy.carden.entity.User;
import com.loopy.carden.service.PracticeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for practice functionality and spaced repetition system
 */
@RestController
@RequestMapping("/v1/practice")
@RequiredArgsConstructor
@Tag(name = "Practice", description = "Spaced repetition practice and review operations")
public class PracticeController {

    private final PracticeService practiceService;

    @PostMapping("/sessions")
    @Operation(summary = "Start a new practice session",
               description = "Creates a new practice session with specified study mode and optional deck filtering")
    public ResponseEntity<StandardResponse<PracticeSessionDto>> startSession(
            @Valid @RequestBody PracticeSessionStartDto startDto,
            @AuthenticationPrincipal User user) {
        
        PracticeSessionDto session = practiceService.startPracticeSession(user.getId(), startDto);
        return ResponseEntity.ok(StandardResponse.success(session));
    }

    @GetMapping("/sessions/current")
    @Operation(summary = "Get current active practice session")
    public ResponseEntity<StandardResponse<PracticeSessionDto>> getCurrentSession(
            @AuthenticationPrincipal User user) {
        
        PracticeSessionDto session = practiceService.getCurrentSession(user.getId());
        return ResponseEntity.ok(StandardResponse.success(session));
    }

    @PostMapping("/sessions/{sessionId}/complete")
    @Operation(summary = "Complete the current practice session")
    public ResponseEntity<StandardResponse<SessionSummaryDto>> completeSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal User user) {
        
        SessionSummaryDto summary = practiceService.completeSession(sessionId, user.getId());
        return ResponseEntity.ok(StandardResponse.success(summary));
    }

    @GetMapping("/due-cards")
    @Operation(summary = "Get due cards for practice",
               description = "Retrieves cards that are due for review based on SRS scheduling")
    public ResponseEntity<StandardResponse<Page<PracticeCardDto>>> getDueCards(
            @Parameter(description = "Deck ID to filter cards (optional)")
            @RequestParam(required = false) Long deckId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<PracticeCardDto> dueCards = practiceService.getDueCards(user.getId(), deckId, pageable);
        return ResponseEntity.ok(StandardResponse.success(dueCards));
    }

    @GetMapping("/next-card")
    @Operation(summary = "Get next card to practice",
               description = "Intelligently selects the next card based on SRS algorithm priority")
    public ResponseEntity<StandardResponse<PracticeCardDto>> getNextCard(
            @Parameter(description = "Deck ID to filter cards (optional)")
            @RequestParam(required = false) Long deckId,
            @AuthenticationPrincipal User user) {
        
        PracticeCardDto nextCard = practiceService.getNextCard(user.getId(), deckId);
        return ResponseEntity.ok(StandardResponse.success(nextCard));
    }

    @PostMapping("/cards/{cardId}/review")
    @Operation(summary = "Submit a card review",
               description = "Processes a card review and updates SRS state based on performance grade (0-3)")
    public ResponseEntity<StandardResponse<ReviewResultDto>> submitReview(
            @PathVariable Long cardId,
            @Valid @RequestBody ReviewSubmissionDto reviewDto,
            @AuthenticationPrincipal User user) {
        
        ReviewResultDto result = practiceService.submitReview(cardId, user.getId(), reviewDto);
        return ResponseEntity.ok(StandardResponse.success(result));
    }



    @GetMapping("/cards/due-count")
    @Operation(summary = "Get count of due cards",
               description = "Returns the number of cards due for review")
    public ResponseEntity<StandardResponse<DueCardsCountDto>> getDueCardsCount(
            @Parameter(description = "Deck ID to filter cards (optional)")
            @RequestParam(required = false) Long deckId,
            @AuthenticationPrincipal User user) {
        
        DueCardsCountDto count = practiceService.getDueCardsCount(user.getId(), deckId);
        return ResponseEntity.ok(StandardResponse.success(count));
    }

    @GetMapping("/cards/new")
    @Operation(summary = "Get new cards for study",
               description = "Retrieves cards that haven't been studied yet")
    public ResponseEntity<StandardResponse<Page<PracticeCardDto>>> getNewCards(
            @Parameter(description = "Deck ID to filter cards (optional)")
            @RequestParam(required = false) Long deckId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<PracticeCardDto> newCards = practiceService.getNewCards(user.getId(), deckId, pageable);
        return ResponseEntity.ok(StandardResponse.success(newCards));
    }

    @GetMapping("/cards/learning")
    @Operation(summary = "Get cards in learning phase",
               description = "Retrieves cards currently in the learning phase (failed cards)")
    public ResponseEntity<StandardResponse<List<PracticeCardDto>>> getLearningCards(
            @Parameter(description = "Deck ID to filter cards (optional)")
            @RequestParam(required = false) Long deckId,
            @AuthenticationPrincipal User user) {
        
        List<PracticeCardDto> learningCards = practiceService.getLearningCards(user.getId(), deckId);
        return ResponseEntity.ok(StandardResponse.success(learningCards));
    }

    @GetMapping("/deck/{deckId}/statistics")
    @Operation(summary = "Get deck-specific practice statistics")
    public ResponseEntity<StandardResponse<DeckStatisticsDto>> getDeckStatistics(
            @PathVariable Long deckId,
            @AuthenticationPrincipal User user) {
        
        DeckStatisticsDto stats = practiceService.getDeckStatistics(user.getId(), deckId);
        return ResponseEntity.ok(StandardResponse.success(stats));
    }

    // ===== Study Mode Specific Endpoints =====

    @GetMapping("/cards/{cardId}/type-answer")
    @Operation(summary = "Get card formatted for type-answer mode",
               description = "Returns a card with type-answer mode specific formatting and hints")
    public ResponseEntity<StandardResponse<TypeAnswerCardDto>> getTypeAnswerCard(
            @PathVariable Long cardId,
            @AuthenticationPrincipal User user) {
        
        TypeAnswerCardDto card = practiceService.getTypeAnswerCard(cardId, user.getId());
        return ResponseEntity.ok(StandardResponse.success(card));
    }

    @GetMapping("/cards/{cardId}/multiple-choice")
    @Operation(summary = "Get card formatted for multiple choice mode",
               description = "Returns a card with multiple choice options and intelligent distractors")
    public ResponseEntity<StandardResponse<MultipleChoiceCardDto>> getMultipleChoiceCard(
            @PathVariable Long cardId,
            @AuthenticationPrincipal User user) {
        
        MultipleChoiceCardDto card = practiceService.getMultipleChoiceCard(cardId, user.getId());
        return ResponseEntity.ok(StandardResponse.success(card));
    }

    @PostMapping("/cards/type-answer/review")
    @Operation(summary = "Submit type-answer mode review",
               description = "Submit user's typed answer for validation and SRS processing")
    public ResponseEntity<StandardResponse<ReviewResultDto>> submitTypeAnswerReview(
            @Valid @RequestBody TypeAnswerSubmissionDto submission,
            @AuthenticationPrincipal User user) {
        
        ReviewResultDto result = practiceService.submitTypeAnswerReview(user.getId(), submission);
        return ResponseEntity.ok(StandardResponse.success(result));
    }

    @PostMapping("/cards/multiple-choice/review")
    @Operation(summary = "Submit multiple choice mode review",
               description = "Submit user's selected option for validation and SRS processing")
    public ResponseEntity<StandardResponse<ReviewResultDto>> submitMultipleChoiceReview(
            @Valid @RequestBody MultipleChoiceSubmissionDto submission,
            @AuthenticationPrincipal User user) {
        
        ReviewResultDto result = practiceService.submitMultipleChoiceReview(user.getId(), submission);
        return ResponseEntity.ok(StandardResponse.success(result));
    }
}
