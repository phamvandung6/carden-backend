package com.loopy.carden.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity implements UserDetails {

    @NotBlank
    @Size(max = 50)
    @Column(unique = true, nullable = false)
    private String username;

    @NotBlank
    @Email
    @Size(max = 100)
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String password;

    @Size(max = 100)
    @Column(name = "display_name")
    private String displayName;

    @Size(max = 500)
    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "email_verified")
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // TTS Settings
    @Column(name = "tts_enabled")
    @Builder.Default
    private boolean ttsEnabled = true;

    @Column(name = "preferred_voice")
    private String preferredVoice;

    @Column(name = "speech_rate")
    @Builder.Default
    private Double speechRate = 1.0;

    @Column(name = "speech_pitch")
    @Builder.Default
    private Double speechPitch = 1.0;

    @Column(name = "speech_volume")
    @Builder.Default
    private Double speechVolume = 1.0;

    // User Preferences
    @Size(max = 32)
    @Column(name = "timezone")
    @Builder.Default
    private String timezone = "UTC";

    @Size(max = 10)
    @Column(name = "ui_language")
    @Builder.Default
    private String uiLanguage = "en";

    @Column(name = "learning_goal_cards_per_day")
    @Builder.Default
    private Integer learningGoalCardsPerDay = 20;

    // Relationships
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Deck> decks;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<StudyState> studyStates;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ReviewSession> reviewSessions;

    // UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    public enum Role {
        USER, ADMIN
    }
}

