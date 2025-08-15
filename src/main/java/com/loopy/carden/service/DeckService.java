package com.loopy.carden.service;

import com.loopy.carden.dto.deck.DeckCreateDto;
import com.loopy.carden.dto.deck.DeckResponseDto;
import com.loopy.carden.dto.deck.DeckUpdateDto;
import com.loopy.carden.entity.Deck;
import com.loopy.carden.entity.User;
import com.loopy.carden.exception.ResourceNotFoundException;
import com.loopy.carden.mapper.DeckMapper;
import com.loopy.carden.repository.DeckRepository;
import com.loopy.carden.repository.DeckSpecifications;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeckService {

    private final DeckRepository deckRepository;
    private final TopicService topicService;

    @Transactional
    public DeckResponseDto createDeck(User owner, DeckCreateDto request) {
        var topic = topicService.getByIdOrThrow(request.getTopicId());
        var deck = DeckMapper.toEntity(request, owner, topic);
        deck.setVisibility(Deck.Visibility.PRIVATE);
        var saved = deckRepository.save(deck);
        return DeckMapper.toResponseDto(saved);
    }

    public DeckResponseDto getDeck(User requester, Long deckId) {
        var deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found: " + deckId));
        if (!canView(requester, deck)) {
            throw new ResourceNotFoundException("Deck not found: " + deckId);
        }
        return DeckMapper.toResponseDto(deck);
    }

    @Transactional
    public DeckResponseDto updateDeck(User requester, Long deckId, DeckUpdateDto request) {
        var deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found: " + deckId));
        requireOwner(requester, deck);
        var topic = topicService.getByIdOrThrow(request.getTopicId());
        DeckMapper.updateEntity(request, deck, topic);
        var saved = deckRepository.save(deck);
        return DeckMapper.toResponseDto(saved);
    }

    @Transactional
    public void deleteDeck(User requester, Long deckId) {
        var deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found: " + deckId));
        requireOwner(requester, deck);
        deck.setDeleted(true);
        deck.setDeletedAt(java.time.LocalDateTime.now());
        deckRepository.save(deck);
    }

    public Page<DeckResponseDto> search(User requester,
                                        String q,
                                        Long topicId,
                                        Deck.CEFRLevel cefr,
                                        boolean publicOnly,
                                        Pageable pageable) {
        // Prefer full-text native search for public browsing
        var page = deckRepository.searchFullTextNative(
                isBlank(q) ? null : q,
                publicOnly,
                topicId,
                cefr != null ? cefr.name() : null,
                pageable
        );
        return page.map(DeckMapper::toResponseDto);
    }

    public Page<DeckResponseDto> searchOwned(User owner, String q, Long topicId, Deck.CEFRLevel cefr, Pageable pageable) {
        Specification<Deck> spec = DeckSpecifications.titleOrDescriptionContains(q)
                .and(DeckSpecifications.hasTopicId(topicId))
                .and(DeckSpecifications.hasCefr(cefr));
        var page = deckRepository.findAll(spec, pageable);
        return page.map(DeckMapper::toResponseDto);
    }

    private boolean canView(User requester, Deck deck) {
        if (deck.getVisibility() == Deck.Visibility.PUBLIC) return true;
        return requester != null && deck.getUser() != null && requester.getId().equals(deck.getUser().getId());
    }

    private void requireOwner(User requester, Deck deck) {
        if (requester == null || deck.getUser() == null || !requester.getId().equals(deck.getUser().getId())) {
            throw new ResourceNotFoundException("Deck not found");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}


