package com.loopy.carden.mapper;

import com.loopy.carden.dto.deck.DeckCreateDto;
import com.loopy.carden.dto.deck.DeckResponseDto;
import com.loopy.carden.dto.deck.DeckUpdateDto;
import com.loopy.carden.entity.Deck;
import com.loopy.carden.entity.Topic;
import com.loopy.carden.entity.User;

public final class DeckMapper {

    private DeckMapper() {}

    public static Deck toEntity(DeckCreateDto dto, User owner, Topic topic) {
        Deck deck = new Deck();
        deck.setTitle(dto.getTitle());
        deck.setDescription(dto.getDescription());
        deck.setUser(owner);
        deck.setTopic(topic);
        deck.setCefrLevel(dto.getCefrLevel());
        deck.setSourceLanguage(dto.getSourceLanguage());
        deck.setTargetLanguage(dto.getTargetLanguage());
        deck.setCoverImageUrl(dto.getCoverImageUrl());
        deck.setTags(dto.getTags());
        // Default to PRIVATE visibility
        deck.setVisibility(Deck.Visibility.PRIVATE);
        return deck;
    }

    public static void updateEntity(DeckUpdateDto dto, Deck deck, Topic topic) {
        if (dto.getTitle() != null) deck.setTitle(dto.getTitle());
        if (dto.getDescription() != null) deck.setDescription(dto.getDescription());
        if (topic != null) deck.setTopic(topic);
        if (dto.getVisibility() != null) {
            deck.setVisibility(dto.getVisibility());
        }
        if (dto.getCefrLevel() != null) deck.setCefrLevel(dto.getCefrLevel());
        if (dto.getSourceLanguage() != null) deck.setSourceLanguage(dto.getSourceLanguage());
        if (dto.getTargetLanguage() != null) deck.setTargetLanguage(dto.getTargetLanguage());
        if (dto.getCoverImageUrl() != null) deck.setCoverImageUrl(dto.getCoverImageUrl());
        if (dto.getTags() != null) deck.setTags(dto.getTags());
    }

    public static DeckResponseDto toResponseDto(Deck deck) {
        return DeckResponseDto.builder()
                .id(deck.getId())
                .title(deck.getTitle())
                .description(deck.getDescription())
                .userId(deck.getUser() != null ? deck.getUser().getId() : null)
                .topicId(deck.getTopic() != null ? deck.getTopic().getId() : null)
                .visibility(deck.getVisibility())
                .cefrLevel(deck.getCefrLevel())
                .sourceLanguage(deck.getSourceLanguage())
                .targetLanguage(deck.getTargetLanguage())
                .coverImageUrl(deck.getCoverImageUrl())
                .tags(deck.getTags())

                .systemDeck(deck.isSystemDeck())
                .downloadCount(deck.getDownloadCount())
                .likeCount(deck.getLikeCount())
                .cardCount(deck.getCardCount())
                .createdAt(deck.getCreatedAt())
                .updatedAt(deck.getUpdatedAt())
                .build();
    }
}


