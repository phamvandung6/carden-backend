package com.loopy.carden.controller;

import com.loopy.carden.dto.StandardResponse;
import com.loopy.carden.dto.deck.DeckCreateDto;
import com.loopy.carden.dto.deck.DeckResponseDto;
import com.loopy.carden.dto.deck.DeckUpdateDto;
import com.loopy.carden.entity.Deck;
import com.loopy.carden.entity.User;
import com.loopy.carden.service.DeckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/decks")
@RequiredArgsConstructor
@Tag(name = "Decks", description = "Deck management")
public class DeckController {

    private final DeckService deckService;

    @GetMapping
    @Operation(
        summary = "Search public decks",
        description = "Search and filter public decks with pagination. " +
                     "Valid sort fields: title, description, createdAt, updatedAt, visibility, cefrLevel. " +
                     "Example: ?sort=title,asc&sort=createdAt,desc"
    )
    public ResponseEntity<StandardResponse<Page<DeckResponseDto>>> searchPublic(@RequestParam(required = false) String q,
                                                              @RequestParam(required = false) Long topicId,
                                                              @RequestParam(required = false) Deck.CEFRLevel cefr,
                                                              Pageable pageable) {
        var result = deckService.search(null, q, topicId, cefr, true, pageable);
        return ResponseEntity.ok(StandardResponse.success(result));
    }

    @GetMapping("/me")
    @Operation(
        summary = "Search my decks",
        description = "Search and filter user's own decks with pagination. " +
                     "Valid sort fields: title, description, createdAt, updatedAt, visibility, cefrLevel. " +
                     "Example: ?sort=title,asc&sort=createdAt,desc"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<StandardResponse<Page<DeckResponseDto>>> searchMine(Authentication authentication,
                                                            @RequestParam(required = false) String q,
                                                            @RequestParam(required = false) Long topicId,
                                                            @RequestParam(required = false) Deck.CEFRLevel cefr,
                                                            Pageable pageable) {
        User user = (User) authentication.getPrincipal();
        var result = deckService.searchOwned(user, q, topicId, cefr, pageable);
        return ResponseEntity.ok(StandardResponse.success(result));
    }

    @PostMapping
    @Operation(summary = "Create a deck")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<StandardResponse<DeckResponseDto>> create(Authentication authentication,
                                                  @Valid @RequestBody DeckCreateDto request) {
        User user = (User) authentication.getPrincipal();
        var created = deckService.createDeck(user, request);
        return ResponseEntity.ok(StandardResponse.success("Deck created", created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get deck by id")
    public ResponseEntity<StandardResponse<DeckResponseDto>> get(Authentication authentication, @PathVariable Long id) {
        User user = authentication != null ? (User) authentication.getPrincipal() : null;
        var dto = deckService.getDeck(user, id);
        return ResponseEntity.ok(StandardResponse.success(dto));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a deck")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<StandardResponse<DeckResponseDto>> update(Authentication authentication,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody DeckUpdateDto request) {
        User user = (User) authentication.getPrincipal();
        var updated = deckService.updateDeck(user, id, request);
        return ResponseEntity.ok(StandardResponse.success("Deck updated", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a deck")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<StandardResponse<Void>> delete(Authentication authentication, @PathVariable Long id) {
        User user = (User) authentication.getPrincipal();
        deckService.deleteDeck(user, id);
        return ResponseEntity.ok(StandardResponse.<Void>builder().success(true).message("Deleted").build());
    }
}


