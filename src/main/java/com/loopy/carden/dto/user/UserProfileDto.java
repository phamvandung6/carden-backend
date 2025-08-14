package com.loopy.carden.dto.user;

import com.loopy.carden.validation.ValidTimezone;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user profile updates.
 * All fields are optional to support partial updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {

    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;

    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    private UiLanguage uiLanguage;

    @ValidTimezone
    @Size(max = 32, message = "Timezone must not exceed 32 characters")
    private String timezone;

    @Min(value = 1, message = "Learning goal must be at least 1 card per day")
    @Max(value = 500, message = "Learning goal must not exceed 500 cards per day")
    private Integer learningGoalCardsPerDay;

    @Size(max = 500, message = "Profile image URL must not exceed 500 characters")
    private String profileImageUrl;
}
