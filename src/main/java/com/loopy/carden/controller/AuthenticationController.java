package com.loopy.carden.controller;

import com.loopy.carden.dto.StandardResponse;
import com.loopy.carden.dto.auth.*;
import com.loopy.carden.entity.User;
import com.loopy.carden.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * REST Controller for user authentication and token management.
 * Provides endpoints for registration, login, logout, token refresh, and user profile retrieval.
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Authentication", description = "User authentication and token management endpoints")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    /**
     * Register a new user
     * @param request the registration request
     * @return authentication response with JWT tokens
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Register new user",
        description = "Create a new user account and return JWT access and refresh tokens"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User successfully registered"),
        @ApiResponse(responseCode = "400", description = "Invalid input or user already exists"),
        @ApiResponse(responseCode = "422", description = "Validation error")
    })
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        
        log.info("Registration request received for email: {}", request.getEmail());
        AuthenticationResponse response = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticate user and return JWT tokens
     * @param request the login request
     * @return authentication response with JWT tokens
     */
    @PostMapping("/login")
    @Operation(
        summary = "Authenticate user",
        description = "Authenticate user credentials and return JWT access and refresh tokens"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User successfully authenticated"),
        @ApiResponse(responseCode = "400", description = "Invalid credentials"),
        @ApiResponse(responseCode = "422", description = "Validation error")
    })
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody LoginRequest request) {
        
        log.info("Authentication request received for: {}", request.getUsernameOrEmail());
        AuthenticationResponse response = authenticationService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Logout user (simple stateless logout)
     * @param authentication the authentication object
     * @return success response
     */
    @PostMapping("/logout")
    @Operation(
        summary = "Logout user",
        description = "Logout user (client-side token invalidation)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User successfully logged out"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<StandardResponse<Void>> logout(Authentication authentication) {
        
        User user = (User) authentication.getPrincipal();
        log.info("Logout request received for user: {}", user.getEmail());
        
        StandardResponse<Void> response = StandardResponse.<Void>builder()
                .success(true)
                .message("Successfully logged out")
                .build();
        
        return ResponseEntity.ok(response);
    }



    /**
     * Health check endpoint for authentication service
     * @return health status
     */
    @GetMapping("/health")
    @Operation(
        summary = "Authentication service health check",
        description = "Check if the authentication service is running"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service is healthy")
    })
    public ResponseEntity<StandardResponse<Void>> health() {
        StandardResponse<Void> response = StandardResponse.<Void>builder()
                .success(true)
                .message("Authentication service is healthy")
                .build();
        
        return ResponseEntity.ok(response);
    }
}
