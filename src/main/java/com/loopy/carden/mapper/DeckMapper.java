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
        return deck;
    }

    public static void updateEntity(DeckUpdateDto dto, Deck deck, Topic topic) {
        if (dto.getTitle() != null) deck.setTitle(dto.getTitle());
        if (dto.getDescription() != null) deck.setDescription(dto.getDescription());
        if (topic != null) deck.setTopic(topic);
        if (dto.getCefrLevel() != null) deck.setCefrLevel(dto.getCefrLevel());
        if (dto.getSourceLanguage() != null) deck.setSourceLanguage(dto.getSourceLanguage());
        if (dto.getTargetLanguage() != null) deck.setTargetLanguage(dto.getTargetLanguage());
        if (dto.getCoverImageUrl() != null) deck.setCoverImageUrl(dto.getCoverImageUrl());
        if (dto.getTags() != null) deck.setTags(dto.getTags());
    }

    public static DeckResponseDto toResponseDto(Deck deck) {
        DeckResponseDto dto = new DeckResponseDto();
        dto.setId(deck.getId());
        dto.setTitle(deck.getTitle());
        dto.setDescription(deck.getDescription());
        dto.setUserId(deck.getUser() != null ? deck.getUser().getId() : null);
        dto.setTopicId(deck.getTopic() != null ? deck.getTopic().getId() : null);
        dto.setVisibility(deck.getVisibility());
        dto.setCefrLevel(deck.getCefrLevel());
        dto.setSourceLanguage(deck.getSourceLanguage());
        dto.setTargetLanguage(deck.getTargetLanguage());
        dto.setCoverImageUrl(deck.getCoverImageUrl());
        dto.setTags(deck.getTags());
        dto.setPublicDeck(deck.isPublic());
        dto.setSystemDeck(deck.isSystemDeck());
        dto.setDownloadCount(deck.getDownloadCount());
        dto.setLikeCount(deck.getLikeCount());
        dto.setCardCount(deck.getCardCount());
        dto.setCreatedAt(deck.getCreatedAt());
        dto.setUpdatedAt(deck.getUpdatedAt());
        return dto;
    }
}


