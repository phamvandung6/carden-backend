package com.loopy.carden.service;

import com.loopy.carden.dto.auth.*;
import com.loopy.carden.entity.User;
import com.loopy.carden.exception.BadRequestException;
import com.loopy.carden.repository.UserRepository;
import com.loopy.carden.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for handling user authentication operations including registration and login.
 * Provides basic user management with JWT access token authentication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Registers a new user
     * @param request the registration request
     * @return authentication response with tokens
     */
    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName() != null ? request.getDisplayName() : request.getUsername());
        user.setRole(User.Role.USER);
        user.setActive(true);
        user.setEmailVerified(false); // In real app, send verification email
        user.setUiLanguage(request.getUiLanguage());
        user.setTimezone(request.getTimezone());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        user = userRepository.save(user);
        log.info("Successfully registered user: {}", user.getEmail());

        // Generate access token
        String accessToken = jwtService.generateAccessToken(user);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration().toSeconds())
                .user(mapToUserInfo(user))
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Authenticates a user
     * @param request the login request
     * @return authentication response with tokens
     */
    @Transactional
    public AuthenticationResponse authenticate(LoginRequest request) {
        log.info("Attempting to authenticate user: {}", request.getUsernameOrEmail());

        try {
            // Authenticate user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(),
                            request.getPassword()
                    )
            );

            // Find user
            User user = userRepository.findByEmailOrUsername(request.getUsernameOrEmail(), request.getUsernameOrEmail())
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

            // Check if user is active
            if (!user.isActive()) {
                throw new BadRequestException("Account is deactivated");
            }

            // Update last login
            user.setLastLogin(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            // Generate access token
            String accessToken = jwtService.generateAccessToken(user);

            log.info("Successfully authenticated user: {}", user.getEmail());

            return AuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getAccessTokenExpiration().toSeconds())
                    .user(mapToUserInfo(user))
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (BadCredentialsException e) {
            log.warn("Failed authentication attempt for: {}", request.getUsernameOrEmail());
            throw new BadRequestException("Invalid username/email or password");
        }
    }



    /**
     * Gets the current user profile
     * @param authentication the authentication object
     * @return user profile response
     */
    public AuthenticationResponse.UserInfo getCurrentUserProfile(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return mapToUserInfo(user);
    }

    /**
     * Maps User entity to UserInfo DTO
     * @param user the user entity
     * @return the user info DTO
     */
    private AuthenticationResponse.UserInfo mapToUserInfo(User user) {
        return AuthenticationResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole().name())
                .emailVerified(user.isEmailVerified())
                .uiLanguage(user.getUiLanguage())
                .timezone(user.getTimezone())
                .build();
    }


}
