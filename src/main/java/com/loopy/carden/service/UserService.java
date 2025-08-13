package com.loopy.carden.service;

import com.loopy.carden.dto.user.TtsSettingsDto;
import com.loopy.carden.dto.user.UserProfileDto;
import com.loopy.carden.entity.User;
import com.loopy.carden.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.loopy.carden.service.storage.CloudflareR2Service;

/**
 * Business logic for user profile management
 */
@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
    private final CloudflareR2Service r2Service;

	@Cacheable(value = "userProfile", key = "#userId")
	public UserProfileDto getProfile(Long userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
		return convertToDto(user);
	}

	@Transactional
	@CacheEvict(value = {"userProfile", "userTts"}, key = "#userId")
	public UserProfileDto updateProfile(Long userId, UserProfileDto dto) {
		User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
		applyProfileDto(dto, user);
		userRepository.save(user);
		return convertToDto(user);
	}

	@Cacheable(value = "userTts", key = "#userId")
	public TtsSettingsDto getTtsSettings(Long userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
		return convertToTtsDto(user);
	}

	@Transactional
	@CacheEvict(value = {"userProfile", "userTts"}, key = "#userId")
	public TtsSettingsDto updateTtsSettings(Long userId, TtsSettingsDto dto) {
		User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
		applyTtsDto(dto, user);
		userRepository.save(user);
		return convertToTtsDto(user);
	}

	@Transactional
	@CacheEvict(value = {"userProfile", "userTts"}, key = "#userId")
	public String uploadAvatar(Long userId, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("File is empty");
		}
		String url = r2Service.uploadAvatar(userId, file);
		User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
		user.setProfileImageUrl(url);
		userRepository.save(user);
		return url;
	}

	@Transactional
	@CacheEvict(value = {"userProfile", "userTts"}, key = "#userId")
	public String confirmAvatarUpload(Long userId, String publicUrl) {
		User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
		user.setProfileImageUrl(publicUrl);
		userRepository.save(user);
		return publicUrl;
	}

	private UserProfileDto convertToDto(User user) {
		return UserProfileDto.builder()
				.displayName(user.getDisplayName())
				.email(user.getEmail())
				.uiLanguage(com.loopy.carden.dto.user.UiLanguage.valueOf(user.getUiLanguage().toUpperCase()))
				.timezone(user.getTimezone())
				.learningGoalCardsPerDay(user.getLearningGoalCardsPerDay())
				.profileImageUrl(user.getProfileImageUrl())
				.build();
	}

	private void applyProfileDto(UserProfileDto dto, User user) {
		user.setDisplayName(dto.getDisplayName());
		user.setEmail(dto.getEmail());
		user.setUiLanguage(dto.getUiLanguage().name().toLowerCase());
		user.setTimezone(dto.getTimezone());
		user.setLearningGoalCardsPerDay(dto.getLearningGoalCardsPerDay());
		// Note: profileImageUrl is not updated here, use avatar endpoints instead
	}

	private TtsSettingsDto convertToTtsDto(User user) {
		return TtsSettingsDto.builder()
				.preferredVoice(user.getPreferredVoice())
				.speechRate(user.getSpeechRate())
				.speechPitch(user.getSpeechPitch())
				.speechVolume(user.getSpeechVolume())
				.ttsEnabled(user.isTtsEnabled())
				.build();
	}

	private void applyTtsDto(TtsSettingsDto dto, User user) {
		user.setPreferredVoice(dto.getPreferredVoice());
		user.setSpeechRate(dto.getSpeechRate());
		user.setSpeechPitch(dto.getSpeechPitch());
		user.setSpeechVolume(dto.getSpeechVolume());
		user.setTtsEnabled(dto.getTtsEnabled());
	}
}


