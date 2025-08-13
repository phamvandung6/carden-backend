package com.loopy.carden.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.ZoneId;

public class ValidTimezoneValidator implements ConstraintValidator<ValidTimezone, String> {
	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null || value.isBlank()) {
			return true; // @NotBlank should handle empties when needed
		}
		try {
			return ZoneId.getAvailableZoneIds().contains(value);
		} catch (Exception ex) {
			return false;
		}
	}
}


