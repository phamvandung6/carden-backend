package com.loopy.carden.service;

import com.loopy.carden.dto.user.TtsSettingsDto;
import com.loopy.carden.dto.user.UserProfileDto;
import com.loopy.carden.entity.User;
import com.loopy.carden.mapper.UserMapper;
import com.loopy.carden.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
// Cache annotations removed temporarily
// import org.springframework.cache.annotation.CacheEvict;
// import org.springframework.cache.annotation.Cacheable;
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

	// @Cacheable(value = "userProfile", key = "#userId")
	public UserProfileDto getProfile(Long userId) {
		User user = findUserOrThrow(userId);
		return UserMapper.toUserProfileDto(user);
	}



	// @Cacheable(value = "userTts", key = "#userId")
	public TtsSettingsDto getTtsSettings(Long userId) {
		User user = findUserOrThrow(userId);
		return UserMapper.toTtsSettingsDto(user);
	}



	@Transactional
	// @CacheEvict(value = {"userProfile", "userTts"}, key = "#userId")
	public UserProfileDto updateProfile(Long userId, UserProfileDto dto) {
		User user = findUserOrThrow(userId);
		UserMapper.updateUserFromProfileDto(dto, user);
		userRepository.save(user);
		return UserMapper.toUserProfileDto(user);
	}

	@Transactional
	// @CacheEvict(value = {"userProfile", "userTts"}, key = "#userId")
	public TtsSettingsDto updateTtsSettings(Long userId, TtsSettingsDto dto) {
		User user = findUserOrThrow(userId);
		UserMapper.updateUserFromTtsDto(dto, user);
		userRepository.save(user);
		return UserMapper.toTtsSettingsDto(user);
	}

	@Transactional
	// @CacheEvict(value = {"userProfile", "userTts"}, key = "#userId")
	public String uploadAvatar(Long userId, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("File is empty");
		}
		String url = r2Service.uploadAvatar(userId, file);
		User user = findUserOrThrow(userId);
		user.setProfileImageUrl(url);
		userRepository.save(user);
		return url;
	}

	@Transactional
	// @CacheEvict(value = {"userProfile", "userTts"}, key = "#userId")
	public String confirmAvatarUpload(Long userId, String publicUrl) {
		User user = findUserOrThrow(userId);
		user.setProfileImageUrl(publicUrl);
		userRepository.save(user);
		return publicUrl;
	}

	// Helper method to reduce code duplication
	private User findUserOrThrow(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new EntityNotFoundException("User not found"));
	}
}


