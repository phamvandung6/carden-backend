package com.loopy.carden.controller;

import com.loopy.carden.dto.StandardResponse;
import com.loopy.carden.dto.deck.DeckCreateDto;
import com.loopy.carden.dto.deck.DeckResponseDto;
import com.loopy.carden.dto.deck.DeckUpdateDto;
import com.loopy.carden.entity.Deck;
import com.loopy.carden.entity.User;
import com.loopy.carden.service.DeckService;
import com.loopy.carden.service.storage.CloudflareR2Service;
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
    private final CloudflareR2Service r2Service;

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
                                                              @Parameter(description = "Page number (0-based)")
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @Parameter(description = "Page size")
                                                              @RequestParam(defaultValue = "20") int size,
                                                              @Parameter(description = "Sort criteria (e.g. title,asc or createdAt,desc)")
                                                              @RequestParam(required = false) String sort) {
        Pageable pageable = createPageable(page, size, sort);
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
                                                            @Parameter(description = "Page number (0-based)")
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @Parameter(description = "Page size")
                                                            @RequestParam(defaultValue = "20") int size,
                                                            @Parameter(description = "Sort criteria (e.g. title,asc or createdAt,desc)")
                                                            @RequestParam(required = false) String sort) {
        User user = (User) authentication.getPrincipal();
        Pageable pageable = createPageable(page, size, sort);
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

    @PostMapping("/{id}/thumbnail/presign")
    @Operation(summary = "Get presigned URL for deck thumbnail upload")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<StandardResponse<CloudflareR2Service.PresignedUpload>> presignThumbnail(
            Authentication authentication,
            @PathVariable Long id,
            @Parameter(description = "Content type of the file (e.g., image/jpeg, image/png)")
            @RequestParam("contentType") String contentType) {
        var presigned = r2Service.createDeckThumbnailPresignedUpload(id, contentType);
        return ResponseEntity.ok(StandardResponse.<CloudflareR2Service.PresignedUpload>builder()
                .success(true)
                .data(presigned)
                .build());
    }

    @PostMapping("/{id}/thumbnail/confirm")
    @Operation(summary = "Confirm deck thumbnail upload and save to deck")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<StandardResponse<String>> confirmThumbnail(
            Authentication authentication,
            @PathVariable Long id,
            @Parameter(description = "Public URL of the uploaded thumbnail")
            @RequestParam("publicUrl") String publicUrl) {
        User user = (User) authentication.getPrincipal();
        String url = deckService.confirmThumbnailUpload(user, id, publicUrl);
        return ResponseEntity.ok(StandardResponse.<String>builder()
                .success(true)
                .message("Deck thumbnail confirmed")
                .data(url)
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


