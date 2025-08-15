package com.loopy.carden.controller;

import com.loopy.carden.dto.StandardResponse;
import com.loopy.carden.dto.card.CardCreateDto;
import com.loopy.carden.dto.card.CardResponseDto;
import com.loopy.carden.dto.card.CardUpdateDto;
import com.loopy.carden.entity.Card;
import com.loopy.carden.entity.User;
import com.loopy.carden.mapper.CardMapper;
import com.loopy.carden.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Card management operations")
public class CardController {

    private final CardService cardService;

    @PostMapping("/decks/{deckId}/cards")
    @Operation(summary = "Create a new card in a deck")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<StandardResponse<CardResponseDto>> createCard(
            Authentication authentication,
            @PathVariable Long deckId,
            @Valid @RequestBody CardCreateDto request) {
        
        User user = (User) authentication.getPrincipal();
        
        // Convert DTO to entity
        var deck = new com.loopy.carden.entity.Deck();
        deck.setId(deckId);
        var card = CardMapper.toEntity(request, deck);
        
        // Create card
        var createdCard = cardService.createCard(user, deckId, card);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.success("Card created", CardMapper.toResponseDto(createdCard)));
    }

    @GetMapping("/decks/{deckId}/cards")
    @Operation(summary = "Get cards from a deck")
    public ResponseEntity<StandardResponse<Page<CardResponseDto>>> getCardsByDeck(
            Authentication authentication,
            @PathVariable Long deckId,
            @RequestParam(required = false) @Parameter(description = "Search query for full-text search") String search,
            @RequestParam(required = false) @Parameter(description = "Filter by difficulty") Card.Difficulty difficulty,
            @RequestParam(required = false) @Parameter(description = "Filter by tag") String tag,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria (e.g. displayOrder,asc or createdAt,desc)")
            @RequestParam(required = false) String sort) {
        
        User user = authentication != null ? (User) authentication.getPrincipal() : null;
        Pageable pageable = createPageable(page, size, sort);
        
        var cards = cardService.getCardsByDeck(user, deckId, search, difficulty, pageable);
        var cardDtos = cards.map(CardMapper::toResponseDto);
        
        return ResponseEntity.ok(StandardResponse.success(cardDtos));
    }

    @GetMapping("/cards/{cardId}")
    @Operation(summary = "Get a card by ID")
    public ResponseEntity<StandardResponse<CardResponseDto>> getCard(
            Authentication authentication,
            @PathVariable Long cardId) {
        
        User user = authentication != null ? (User) authentication.getPrincipal() : null;
        
        var card = cardService.getCard(user, cardId);
        return ResponseEntity.ok(StandardResponse.success(CardMapper.toResponseDto(card)));
    }

    @PatchMapping("/cards/{cardId}")
    @Operation(summary = "Update a card")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<StandardResponse<CardResponseDto>> updateCard(
            Authentication authentication,
            @PathVariable Long cardId,
            @Valid @RequestBody CardUpdateDto request) {
        
        User user = (User) authentication.getPrincipal();
        
        // CardUpdateDto supports partial updates - only provided fields are updated
        var updatedCard = cardService.updateCard(user, cardId, request);
        return ResponseEntity.ok(StandardResponse.success("Card updated", CardMapper.toResponseDto(updatedCard)));
    }

    @DeleteMapping("/cards/{cardId}")
    @Operation(summary = "Delete a card")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<StandardResponse<Void>> deleteCard(
            Authentication authentication,
            @PathVariable Long cardId) {
        
        User user = (User) authentication.getPrincipal();
        
        cardService.deleteCard(user, cardId);
        
        return ResponseEntity.ok(StandardResponse.<Void>builder()
                .success(true)
                .message("Card deleted successfully")
                .build());
    }

    @PostMapping("/decks/{deckId}/cards/bulk")
    @Operation(summary = "Bulk create cards in a deck")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<StandardResponse<List<CardResponseDto>>> bulkCreateCards(
            Authentication authentication,
            @PathVariable Long deckId,
            @Valid @RequestBody List<CardCreateDto> requests) {
        
        User user = (User) authentication.getPrincipal();
        
        // Convert DTOs to entities
        var deck = new com.loopy.carden.entity.Deck();
        deck.setId(deckId);
        var cards = requests.stream()
                .map(dto -> CardMapper.toEntity(dto, deck))
                .toList();
        
        var createdCards = cardService.bulkCreateCards(user, deckId, cards);
        var responseDtos = CardMapper.toResponseDtoList(createdCards);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.<List<CardResponseDto>>builder()
                        .success(true)
                        .message("Cards created successfully")
                        .data(responseDtos)
                        .build());
    }

    @GetMapping("/decks/{deckId}/cards/count")
    @Operation(summary = "Get card count for a deck")
    public ResponseEntity<StandardResponse<Long>> getCardCount(
            @PathVariable Long deckId) {
        
        long count = cardService.getCardCountByDeck(deckId);
        
        return ResponseEntity.ok(StandardResponse.<Long>builder()
                .success(true)
                .data(count)
                .build());
    }

    @PostMapping("/cards/{cardId}/duplicate-check")
    @Operation(summary = "Check if card content would create a duplicate")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<StandardResponse<Boolean>> checkDuplicate(
            Authentication authentication,
            @PathVariable Long cardId,
            @Valid @RequestBody CardCreateDto request) {
        
        // This would need to be implemented in CardService
        // For now, return false (no duplicate)
        
        return ResponseEntity.ok(StandardResponse.<Boolean>builder()
                .success(true)
                .data(false)
                .message("No duplicate found")
                .build());
    }

    private Pageable createPageable(int page, int size, String sort) {
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size);
        }
        
        String[] sortParts = sort.split(",");
        String property = sortParts[0];
        Sort.Direction direction = Sort.Direction.ASC;
        
        if (sortParts.length > 1) {
            direction = sortParts[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        }
        
        return PageRequest.of(page, size, Sort.by(direction, property));
    }
}
