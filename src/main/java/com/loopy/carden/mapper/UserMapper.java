package com.loopy.carden.mapper;

import com.loopy.carden.dto.user.TtsSettingsDto;
import com.loopy.carden.dto.user.UiLanguage;
import com.loopy.carden.dto.user.UserProfileDto;
import com.loopy.carden.entity.User;

public final class UserMapper {

    private UserMapper() {}

    public static UserProfileDto toUserProfileDto(User user) {
        return UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .emailVerified(user.isEmailVerified())
                .uiLanguage(UiLanguage.valueOf(user.getUiLanguage().toUpperCase()))
                .timezone(user.getTimezone())
                .learningGoalCardsPerDay(user.getLearningGoalCardsPerDay())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }



    public static TtsSettingsDto toTtsSettingsDto(User user) {
        return TtsSettingsDto.builder()
                .preferredVoice(user.getPreferredVoice())
                .speechRate(user.getSpeechRate())
                .speechPitch(user.getSpeechPitch())
                .speechVolume(user.getSpeechVolume())
                .ttsEnabled(user.isTtsEnabled())
                .build();
    }



    public static void updateUserFromProfileDto(UserProfileDto dto, User user) {
        if (dto.getDisplayName() != null) user.setDisplayName(dto.getDisplayName());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getUiLanguage() != null) user.setUiLanguage(dto.getUiLanguage().name().toLowerCase());
        if (dto.getTimezone() != null) user.setTimezone(dto.getTimezone());
        if (dto.getLearningGoalCardsPerDay() != null) user.setLearningGoalCardsPerDay(dto.getLearningGoalCardsPerDay());
        // Note: profileImageUrl is not updated here, use avatar endpoints instead
    }

    public static void updateUserFromTtsDto(TtsSettingsDto dto, User user) {
        if (dto.getPreferredVoice() != null) user.setPreferredVoice(dto.getPreferredVoice());
        if (dto.getSpeechRate() != null) user.setSpeechRate(dto.getSpeechRate());
        if (dto.getSpeechPitch() != null) user.setSpeechPitch(dto.getSpeechPitch());
        if (dto.getSpeechVolume() != null) user.setSpeechVolume(dto.getSpeechVolume());
        if (dto.getTtsEnabled() != null) user.setTtsEnabled(dto.getTtsEnabled());
    }
}
