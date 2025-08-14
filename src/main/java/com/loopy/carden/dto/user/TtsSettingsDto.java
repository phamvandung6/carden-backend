package com.loopy.carden.dto.user;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for TTS settings updates.
 * All fields are optional to support partial updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsSettingsDto {

    @Size(max = 100, message = "Preferred voice must not exceed 100 characters")
    private String preferredVoice;

    @DecimalMin(value = "0.5", message = "Speech rate must be at least 0.5")
    @DecimalMax(value = "2.0", message = "Speech rate must not exceed 2.0")
    private Double speechRate;

    @DecimalMin(value = "0.5", message = "Speech pitch must be at least 0.5")
    @DecimalMax(value = "2.0", message = "Speech pitch must not exceed 2.0")
    private Double speechPitch;

    @DecimalMin(value = "0.0", message = "Speech volume must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Speech volume must not exceed 1.0")
    private Double speechVolume;

    private Boolean ttsEnabled;
}
