package com.loopy.carden.dto.user;

import com.loopy.carden.validation.ValidTimezone;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the editable parts of a user's profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {

	@NotBlank
	@Size(max = 100)
	private String displayName;

	@NotBlank
	@Email
	@Size(max = 100)
	private String email;

	@NotNull
	private UiLanguage uiLanguage;

	@NotBlank
	@ValidTimezone
	@Size(max = 32)
	private String timezone;

	@NotNull
	@Min(1)
	@Max(500)
	private Integer learningGoalCardsPerDay;

	@Size(max = 500)
	private String profileImageUrl;
}


