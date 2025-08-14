package com.loopy.carden.mapper;

import com.loopy.carden.dto.card.CardCreateDto;
import com.loopy.carden.dto.card.CardResponseDto;
import com.loopy.carden.dto.card.CardUpdateDto;
import com.loopy.carden.entity.Card;
import com.loopy.carden.entity.Deck;

import java.util.ArrayList;
import java.util.List;

public final class CardMapper {

    private CardMapper() {}

    public static Card toEntity(CardCreateDto dto, Deck deck) {
        Card card = new Card();
        card.setDeck(deck);
        card.setFront(dto.getFront());
        card.setBack(dto.getBack());
        card.setIpaPronunciation(dto.getIpaPronunciation());
        card.setImageUrl(dto.getImageUrl());
        card.setAudioUrl(dto.getAudioUrl());
        card.setExamples(dto.getExamples() != null ? new ArrayList<>(dto.getExamples()) : null);
        card.setSynonyms(dto.getSynonyms() != null ? new ArrayList<>(dto.getSynonyms()) : null);
        card.setAntonyms(dto.getAntonyms() != null ? new ArrayList<>(dto.getAntonyms()) : null);
        card.setTags(dto.getTags() != null ? new ArrayList<>(dto.getTags()) : null);
        card.setDifficulty(dto.getDifficulty() != null ? dto.getDifficulty() : Card.Difficulty.NORMAL);
        card.setDisplayOrder(dto.getDisplayOrder());
        return card;
    }

    public static void updateEntity(CardUpdateDto dto, Card card) {
        if (dto.getFront() != null) card.setFront(dto.getFront());
        if (dto.getBack() != null) card.setBack(dto.getBack());
        if (dto.getIpaPronunciation() != null) card.setIpaPronunciation(dto.getIpaPronunciation());
        if (dto.getImageUrl() != null) card.setImageUrl(dto.getImageUrl());
        if (dto.getAudioUrl() != null) card.setAudioUrl(dto.getAudioUrl());
        if (dto.getExamples() != null) card.setExamples(new ArrayList<>(dto.getExamples()));
        if (dto.getSynonyms() != null) card.setSynonyms(new ArrayList<>(dto.getSynonyms()));
        if (dto.getAntonyms() != null) card.setAntonyms(new ArrayList<>(dto.getAntonyms()));
        if (dto.getTags() != null) card.setTags(new ArrayList<>(dto.getTags()));
        if (dto.getDifficulty() != null) card.setDifficulty(dto.getDifficulty());
        if (dto.getDisplayOrder() != null) card.setDisplayOrder(dto.getDisplayOrder());
    }

    public static CardResponseDto toResponseDto(Card card) {
        return CardResponseDto.builder()
                .id(card.getId())
                .deckId(card.getDeck() != null ? card.getDeck().getId() : null)
                .front(card.getFront())
                .back(card.getBack())
                .ipaPronunciation(card.getIpaPronunciation())
                .imageUrl(card.getImageUrl())
                .audioUrl(card.getAudioUrl())
                .examples(card.getExamples() != null ? new ArrayList<>(card.getExamples()) : null)
                .synonyms(card.getSynonyms() != null ? new ArrayList<>(card.getSynonyms()) : null)
                .antonyms(card.getAntonyms() != null ? new ArrayList<>(card.getAntonyms()) : null)
                .tags(card.getTags() != null ? new ArrayList<>(card.getTags()) : null)
                .difficulty(card.getDifficulty())
                .displayOrder(card.getDisplayOrder())
                .uniqueKey(card.getUniqueKey())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .studyStats(mapStudyStats(card)) // Will be null for now
                .build();
    }

    public static List<CardResponseDto> toResponseDtoList(List<Card> cards) {
        return cards.stream()
                .map(CardMapper::toResponseDto)
                .toList();
    }

    // Helper method for future study stats integration
    private static CardResponseDto.StudyStats mapStudyStats(Card card) {
        // This would be implemented when StudyState integration is ready
        // For now, return null or empty stats
        return null;
    }
}
