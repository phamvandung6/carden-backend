package com.loopy.carden.dto.user;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Text-to-Speech settings management
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsSettingsDto {

	@Size(max = 100)
	private String preferredVoice;

	@NotNull
	@DecimalMin(value = "0.5")
	@DecimalMax(value = "2.0")
	private Double speechRate;

	@NotNull
	@DecimalMin(value = "0.5")
	@DecimalMax(value = "2.0")
	private Double speechPitch;

	@NotNull
	@DecimalMin(value = "0.0")
	@DecimalMax(value = "1.0")
	private Double speechVolume;

	@NotNull
	private Boolean ttsEnabled;
}


