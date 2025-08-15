package com.loopy.carden.controller;

import com.loopy.carden.dto.StandardResponse;
import com.loopy.carden.dto.user.TtsSettingsDto;
import com.loopy.carden.dto.user.UserProfileDto;
import com.loopy.carden.entity.User;
import com.loopy.carden.service.storage.CloudflareR2Service;
import com.loopy.carden.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile management")
public class UserController {

    private final UserService userService;
    private final CloudflareR2Service r2Service;

	@GetMapping("/me")
	@Operation(summary = "Get current user profile")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Profile retrieved"),
		@ApiResponse(responseCode = "401", description = "Unauthorized")
	})
	@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
	public ResponseEntity<StandardResponse<UserProfileDto>> getMe(Authentication authentication) {
		User user = (User) authentication.getPrincipal();
		return ResponseEntity.ok(StandardResponse.success(userService.getProfile(user.getId())));
	}

	@PatchMapping("/me")
	@Operation(summary = "Update current user profile")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Profile updated"),
		@ApiResponse(responseCode = "400", description = "Validation error"),
		@ApiResponse(responseCode = "401", description = "Unauthorized")
	})
	@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
	public ResponseEntity<StandardResponse<UserProfileDto>> updateMe(Authentication authentication,
	                                             @Valid @RequestBody UserProfileDto request) {
		User user = (User) authentication.getPrincipal();
		return ResponseEntity.ok(StandardResponse.success(userService.updateProfile(user.getId(), request)));
	}

	@PatchMapping("/me/tts-settings")
	@Operation(summary = "Update TTS settings")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "TTS settings updated"),
		@ApiResponse(responseCode = "400", description = "Validation error"),
		@ApiResponse(responseCode = "401", description = "Unauthorized")
	})
	@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
	public ResponseEntity<StandardResponse<TtsSettingsDto>> updateTts(Authentication authentication,
	                                               @Valid @RequestBody TtsSettingsDto request) {
		User user = (User) authentication.getPrincipal();
		return ResponseEntity.ok(StandardResponse.success(userService.updateTtsSettings(user.getId(), request)));
	}

	@GetMapping("/me/tts-settings")
	@Operation(summary = "Get current user TTS settings")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "TTS settings retrieved"),
		@ApiResponse(responseCode = "401", description = "Unauthorized")
	})
	@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
	public ResponseEntity<StandardResponse<TtsSettingsDto>> getTtsSettings(Authentication authentication) {
		User user = (User) authentication.getPrincipal();
		return ResponseEntity.ok(StandardResponse.success(userService.getTtsSettings(user.getId())));
	}

    @PostMapping(value = "/me/avatar", consumes = "multipart/form-data")
    @Operation(summary = "Upload profile picture (server-side)")
	@SecurityRequirement(name = "bearerAuth")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Avatar uploaded"),
		@ApiResponse(responseCode = "400", description = "Validation error"),
		@ApiResponse(responseCode = "401", description = "Unauthorized")
	})
	@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
	public ResponseEntity<StandardResponse<String>> uploadAvatar(Authentication authentication,
																 @Parameter(description = "Avatar image file", 
																 content = @Content(mediaType = "multipart/form-data"))
																 @RequestParam("file") MultipartFile file) {
		User user = (User) authentication.getPrincipal();
		String url = userService.uploadAvatar(user.getId(), file);
		return ResponseEntity.ok(StandardResponse.<String>builder().success(true).message("Uploaded").data(url).build());
	}

    @PostMapping("/me/avatar/presign")
    @Operation(summary = "Get presigned URL for avatar upload")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Presigned URL generated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<StandardResponse<CloudflareR2Service.PresignedUpload>> presignAvatar(Authentication authentication,
                                                                        @Parameter(description = "Content type of the file (e.g., image/jpeg, image/png)")
                                                                        @RequestParam("contentType") String contentType) {
        User user = (User) authentication.getPrincipal();
        var presigned = r2Service.createAvatarPresignedUpload(user.getId(), contentType);
        return ResponseEntity.ok(StandardResponse.<CloudflareR2Service.PresignedUpload>builder().success(true).data(presigned).build());
    }

    @PostMapping("/me/avatar/confirm")
    @Operation(summary = "Confirm avatar upload and save to profile")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avatar confirmed and saved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<StandardResponse<String>> confirmAvatar(Authentication authentication,
                                                                 @Parameter(description = "Public URL of the uploaded avatar")
                                                                 @RequestParam("publicUrl") String publicUrl) {
        User user = (User) authentication.getPrincipal();
        String url = userService.confirmAvatarUpload(user.getId(), publicUrl);
        return ResponseEntity.ok(StandardResponse.<String>builder().success(true).message("Avatar confirmed").data(url).build());
    }
}


