package com.loopy.carden.service;

import com.loopy.carden.dto.card.CardUpdateDto;
import com.loopy.carden.entity.Card;
import com.loopy.carden.entity.Deck;
import com.loopy.carden.entity.User;
import com.loopy.carden.exception.BadRequestException;
import com.loopy.carden.exception.ResourceNotFoundException;
import com.loopy.carden.mapper.CardMapper;
import com.loopy.carden.repository.CardRepository;
import com.loopy.carden.repository.CardSpecifications;
import com.loopy.carden.repository.DeckRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
// Cache annotations removed temporarily
// import org.springframework.cache.annotation.CacheEvict;
// import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final DeckRepository deckRepository;

    @Transactional
    public Card createCard(User owner, Long deckId, Card card) {
        // Verify deck ownership
        var deck = getDeckAndVerifyOwnership(owner, deckId);
        card.setDeck(deck);
        
        // Generate unique key for duplicate detection
        String uniqueKey = generateUniqueKey(card.getFront(), card.getBack());
        card.setUniqueKey(uniqueKey);
        
        // Check for duplicates
        checkForDuplicates(deck, uniqueKey, null);
        
        // Set display order
        if (card.getDisplayOrder() == null || card.getDisplayOrder() == 0) {
            card.setDisplayOrder(getNextDisplayOrder(deck));
        }
        
        var saved = cardRepository.save(card);
        
        // Update deck card count
        updateDeckCardCount(deck);
        
        return saved;
    }

    // @Cacheable(value = "cards", key = "#cardId")
    public Card getCard(User requester, Long cardId) {
        var card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardId));
        
        // Verify access to the deck
        verifyDeckAccess(requester, card.getDeck());
        
        return card;
    }

    public Page<Card> getCardsByDeck(User requester, Long deckId, String search, Card.Difficulty difficulty, Pageable pageable) {
        var deck = getDeckAndVerifyAccess(requester, deckId);
        
        if (search != null && !search.isBlank()) {
            // Use full-text search
            return cardRepository.searchFullTextNative(
                deckId, 
                search, 
                difficulty != null ? difficulty.name() : null, 
                pageable
            );
        } else {
            // Use JPA Specifications
            Specification<Card> spec = CardSpecifications.belongsToDeck(deck)
                    .and(CardSpecifications.hasDifficulty(difficulty));
            return cardRepository.findAll(spec, pageable);
        }
    }

    @Transactional
    // @CacheEvict(value = "cards", key = "#cardId")
    public Card updateCard(User owner, Long cardId, CardUpdateDto updateDto) {
        var existingCard = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardId));
        
        // Verify deck ownership
        verifyDeckOwnership(owner, existingCard.getDeck());
        
        // Store original values for duplicate checking
        String originalFront = existingCard.getFront();
        String originalBack = existingCard.getBack();
        
        // Use CardMapper to update fields - this replaces all the manual mapping
        CardMapper.updateEntity(updateDto, existingCard);
        
        // Check for duplicates if front or back changed
        boolean frontChanged = updateDto.getFront() != null && !originalFront.equals(existingCard.getFront());
        boolean backChanged = updateDto.getBack() != null && !originalBack.equals(existingCard.getBack());
        
        if (frontChanged || backChanged) {
            String newUniqueKey = generateUniqueKey(existingCard.getFront(), existingCard.getBack());
            if (!newUniqueKey.equals(existingCard.getUniqueKey())) {
                checkForDuplicates(existingCard.getDeck(), newUniqueKey, existingCard.getId());
                existingCard.setUniqueKey(newUniqueKey);
            }
        }
        
        return cardRepository.save(existingCard);
    }

    @Transactional
    // @CacheEvict(value = "cards", key = "#cardId")
    public void deleteCard(User owner, Long cardId) {
        var card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardId));
        
        // Verify deck ownership
        verifyDeckOwnership(owner, card.getDeck());
        
        // Soft delete
        card.setDeleted(true);
        card.setDeletedAt(LocalDateTime.now());
        cardRepository.save(card);
        
        // Update deck card count
        updateDeckCardCount(card.getDeck());
    }

    @Transactional
    public List<Card> bulkCreateCards(User owner, Long deckId, List<Card> cards) {
        var deck = getDeckAndVerifyOwnership(owner, deckId);
        
        // Process each card
        for (Card card : cards) {
            card.setDeck(deck);
            String uniqueKey = generateUniqueKey(card.getFront(), card.getBack());
            card.setUniqueKey(uniqueKey);
            
            // Check for duplicates (skip duplicates in bulk operation)
            if (cardRepository.findByDeckAndUniqueKey(deck, uniqueKey).isPresent()) {
                continue; // Skip duplicate
            }
            
            if (card.getDisplayOrder() == null || card.getDisplayOrder() == 0) {
                card.setDisplayOrder(getNextDisplayOrder(deck));
            }
        }
        
        var savedCards = cardRepository.saveAll(cards);
        updateDeckCardCount(deck);
        
        return savedCards;
    }

    public long getCardCountByDeck(Long deckId) {
        return cardRepository.countByDeckId(deckId);
    }

    @Transactional
    public String confirmImageUpload(User owner, Long cardId, String publicUrl) {
        var card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardId));
        
        // Verify deck ownership
        verifyDeckOwnership(owner, card.getDeck());
        
        // Update card with new image URL
        card.setImageUrl(publicUrl);
        cardRepository.save(card);
        
        return publicUrl;
    }

    // Helper methods
    private String generateUniqueKey(String front, String back) {
        if (front == null || back == null) {
            throw new BadRequestException("Front and back text are required");
        }
        
        // Normalize text for duplicate detection
        String normalizedFront = normalizeText(front);
        String normalizedBack = normalizeText(back);
        
        return String.format("%s:%s", normalizedFront, normalizedBack);
    }

    private String normalizeText(String text) {
        return text.toLowerCase()
                .trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[^\\p{L}\\p{N}\\s]", ""); // Remove special characters, keep letters, numbers, spaces
    }

    private void checkForDuplicates(Deck deck, String uniqueKey, Long excludeId) {
        var duplicate = excludeId != null 
            ? cardRepository.findDuplicateCard(deck, uniqueKey, excludeId)
            : cardRepository.findByDeckAndUniqueKey(deck, uniqueKey);
            
        if (duplicate.isPresent()) {
            throw new BadRequestException("A card with similar content already exists in this deck");
        }
    }

    private int getNextDisplayOrder(Deck deck) {
        return (int) cardRepository.countByDeckId(deck.getId()) + 1;
    }

    private void updateDeckCardCount(Deck deck) {
        long cardCount = cardRepository.countByDeckId(deck.getId());
        deck.setCardCount((int) cardCount);
        // Note: This would require DeckRepository.save(deck) but we don't want circular dependency
        // This should be handled by a separate service or event
    }

    private Deck getDeckAndVerifyOwnership(User owner, Long deckId) {
        var deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found: " + deckId));
        verifyDeckOwnership(owner, deck);
        return deck;
    }

    private Deck getDeckAndVerifyAccess(User requester, Long deckId) {
        var deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found: " + deckId));
        verifyDeckAccess(requester, deck);
        return deck;
    }

    private void verifyDeckOwnership(User owner, Deck deck) {
        if (!deck.getUser().getId().equals(owner.getId())) {
            throw new ResourceNotFoundException("Deck not found");
        }
    }

    private void verifyDeckAccess(User requester, Deck deck) {
        // Check if user owns the deck or if deck is public
        if (!deck.getUser().getId().equals(requester.getId()) && 
            deck.getVisibility() != Deck.Visibility.PUBLIC) {
            throw new ResourceNotFoundException("Deck not found");
        }
    }
}
