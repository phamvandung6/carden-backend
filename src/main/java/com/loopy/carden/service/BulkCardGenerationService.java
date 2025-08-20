package com.loopy.carden.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopy.carden.dto.card.BulkCardGenerationRequestDto;
import com.loopy.carden.dto.card.BulkCardGenerationResponseDto;
import com.loopy.carden.entity.Card;
import com.loopy.carden.entity.Deck;
import com.loopy.carden.entity.User;
import com.loopy.carden.exception.BadRequestException;
import com.loopy.carden.exception.ResourceNotFoundException;
import com.loopy.carden.exception.ServiceException;
import com.loopy.carden.repository.DeckRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkCardGenerationService {

    private final DeckRepository deckRepository;
    private final CardService cardService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${carden.python.service.url:http://localhost:8001}")
    private String pythonServiceUrl;

    @Value("${carden.python.service.enabled:true}")
    private boolean pythonServiceEnabled;

    @Transactional
    public BulkCardGenerationResponseDto generateCards(User user, BulkCardGenerationRequestDto request) {
        long startTime = System.currentTimeMillis();
        
        log.info("Starting bulk card generation for user {} deck {} with topic: {}", 
                user.getId(), request.getDeckId(), request.getTopic());

        try {
            // Validate deck ownership and access
            Deck deck = validateDeckAccess(user, request.getDeckId());
            
            // Check if Python service is enabled
            if (!pythonServiceEnabled) {
                throw new ServiceException("Bulk card generation service is currently disabled");
            }
            
            // Prepare request for Python service using deck information
            Map<String, Object> pythonRequest = buildPythonRequest(request, deck);
            
            // Call Python service to generate cards
            Map<String, Object> pythonResponse = callPythonCardGeneration(pythonRequest);
            
            // Parse and validate response
            BulkCardGenerationResponseDto response = parsePythonResponse(pythonResponse, request, deck, startTime);
            
            log.info("Bulk card generation completed for deck {} - {} cards saved", 
                    request.getDeckId(), response.getTotalSaved());
            
            return response;
            
        } catch (Exception e) {
            log.error("Error during bulk card generation for deck {}: {}", request.getDeckId(), e.getMessage(), e);
            
            return BulkCardGenerationResponseDto.builder()
                    .success(false)
                    .message("Failed to generate cards: " + e.getMessage())
                    .totalRequested(request.getCount())
                    .totalGenerated(0)
                    .totalSaved(0)
                    .deckId(request.getDeckId())
                    .errors(List.of(e.getMessage()))
                    .processedAt(LocalDateTime.now())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private Deck validateDeckAccess(User user, Long deckId) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found with ID: " + deckId));

        if (!deck.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to modify this deck");
        }

        if (deck.isDeleted()) {
            throw new BadRequestException("Cannot add cards to a deleted deck");
        }

        return deck;
    }

    private Map<String, Object> buildPythonRequest(BulkCardGenerationRequestDto request, Deck deck) {
        Map<String, Object> pythonRequest = new HashMap<>();
        pythonRequest.put("deck_id", request.getDeckId());
        pythonRequest.put("topic", request.getTopic());
        pythonRequest.put("count", request.getCount());
        
        // Use deck's language settings
        pythonRequest.put("source_language", deck.getSourceLanguage() != null ? deck.getSourceLanguage() : "en");
        pythonRequest.put("target_language", deck.getTargetLanguage() != null ? deck.getTargetLanguage() : "vi");
        
        // Use deck's CEFR level if available
        if (deck.getCefrLevel() != null) {
            pythonRequest.put("cefr_level", deck.getCefrLevel().name());
        }
        
        if (request.getKeywords() != null && !request.getKeywords().isEmpty()) {
            pythonRequest.put("keywords", request.getKeywords());
        }
        
        pythonRequest.put("include_examples", request.getIncludeExamples());
        pythonRequest.put("include_pronunciation", request.getIncludePronunciation());
        
        if (request.getAdditionalContext() != null && !request.getAdditionalContext().trim().isEmpty()) {
            pythonRequest.put("additional_context", request.getAdditionalContext());
        }
        
        return pythonRequest;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callPythonCardGeneration(Map<String, Object> request) {
        try {
            String url = pythonServiceUrl + "/generate-cards";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            log.debug("Calling Python service at {} with request: {}", url, request);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );
            
            // Accept both 200 (success) and 400 (validation failure) as valid responses
            if (response.getStatusCode() != HttpStatus.OK && response.getStatusCode() != HttpStatus.BAD_REQUEST) {
                throw new ServiceException("Python service returned unexpected error: " + response.getStatusCode());
            }
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new ServiceException("Python service returned empty response");
            }
            
            log.debug("Python service response: {}", responseBody);
            return responseBody;
            
        } catch (HttpClientErrorException e) {
            // Handle HTTP 400 (validation failures) gracefully
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.warn("Python service validation failure: {}", e.getResponseBodyAsString());
                
                // Parse the error response to extract validation feedback
                try {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "Content validation failed");
                    errorResponse.put("detail", e.getResponseBodyAsString());
                    errorResponse.put("generated_cards", List.of());
                    errorResponse.put("total_count", 0);
                    errorResponse.put("errors", List.of("Content validation failed: " + e.getResponseBodyAsString()));
                    
                    return errorResponse;
                } catch (Exception parseEx) {
                    log.error("Failed to parse validation error response", parseEx);
                    throw new ServiceException("Content validation failed: " + e.getResponseBodyAsString());
                }
            } else {
                throw new ServiceException("Failed to communicate with card generation service: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("Failed to call Python card generation service: {}", e.getMessage(), e);
            throw new ServiceException("Failed to communicate with card generation service: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private BulkCardGenerationResponseDto parsePythonResponse(
            Map<String, Object> pythonResponse, 
            BulkCardGenerationRequestDto originalRequest,
            Deck deck,
            long startTime) {
        
        try {
            Boolean success = (Boolean) pythonResponse.get("success");
            String message = (String) pythonResponse.get("message");
            Integer totalGenerated = (Integer) pythonResponse.get("total_count");
            Integer totalSaved = (Integer) pythonResponse.get("total_saved");
            Integer duplicatesSkipped = (Integer) pythonResponse.get("duplicates_skipped");
            
            List<String> errors = null;
            Object errorsObj = pythonResponse.get("errors");
            if (errorsObj instanceof List) {
                errors = (List<String>) errorsObj;
            }
            
            // Check if this is a validation failure with detail
            String detail = (String) pythonResponse.get("detail");
            if (detail != null && !success) {
                // Extract meaningful validation message from detail
                try {
                    // Parse JSON from detail if it's a JSON string
                    if (detail.startsWith("{") && detail.endsWith("}")) {
                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, Object> detailMap = mapper.readValue(detail, Map.class);
                        String detailMessage = (String) detailMap.get("detail");
                        if (detailMessage != null) {
                            message = detailMessage;
                        }
                    } else {
                        message = detail;
                    }
                } catch (Exception parseEx) {
                    log.debug("Could not parse detail as JSON, using as plain text: {}", detail);
                    message = detail;
                }
            }
            
            // Build summary using deck information
            BulkCardGenerationResponseDto.GenerationSummary summary = 
                    BulkCardGenerationResponseDto.GenerationSummary.builder()
                            .topic(originalRequest.getTopic())
                            .sourceLanguage(deck.getSourceLanguage() != null ? deck.getSourceLanguage() : "en")
                            .targetLanguage(deck.getTargetLanguage() != null ? deck.getTargetLanguage() : "vi")
                            .cefrLevel(deck.getCefrLevel() != null ? deck.getCefrLevel().name() : null)
                            .duplicatesSkipped(duplicatesSkipped != null ? duplicatesSkipped : 0)
                            .validationErrors(success != null && !success ? 1 : 0)   // Mark validation error
                            .build();
            
            return BulkCardGenerationResponseDto.builder()
                    .success(success != null ? success : false)
                    .message(message != null ? message : "Unknown response from generation service")
                    .totalRequested(originalRequest.getCount())
                    .totalGenerated(totalGenerated != null ? totalGenerated : 0)
                    .totalSaved(totalSaved != null ? totalSaved : 0)
                    .deckId(originalRequest.getDeckId())
                    .errors(errors)
                    .processedAt(LocalDateTime.now())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .summary(summary)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to parse Python service response: {}", e.getMessage(), e);
            throw new ServiceException("Failed to parse card generation response: " + e.getMessage(), e);
        }
    }

    public boolean isServiceAvailable() {
        try {
            String healthUrl = pythonServiceUrl + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(healthUrl, Map.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("Python card generation service is not available: {}", e.getMessage());
            return false;
        }
    }
}
