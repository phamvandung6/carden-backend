package com.loopy.carden.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "cards", indexes = {
    @Index(name = "idx_card_deck_id", columnList = "deck_id"),
    @Index(name = "idx_card_unique_key", columnList = "unique_key")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted = false")
public class Card extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false)
    private String front;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false)
    private String back;

    @Size(max = 200)
    @Column(name = "ipa_pronunciation")
    private String ipaPronunciation;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> examples;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> synonyms;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> antonyms;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> tags;

    @Size(max = 500)
    @Column(name = "image_url")
    private String imageUrl;

    @Size(max = 500)
    @Column(name = "audio_url")
    private String audioUrl;

    @Column(name = "unique_key", nullable = false)
    private String uniqueKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty = Difficulty.NORMAL;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // Audit timestamp

    // Relationships
    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<StudyState> studyStates;

    public enum Difficulty {
        EASY, NORMAL, HARD
    }

    // Helper methods
    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
    }

    @PrePersist
    @PreUpdate
    private void generateUniqueKey() {
        if (this.uniqueKey == null) {
            this.uniqueKey = generateCardUniqueKey();
        }
    }

    private String generateCardUniqueKey() {
        // Normalize text for duplicate detection
        String normalizedFront = front.toLowerCase().trim().replaceAll("\\s+", " ");
        String normalizedBack = back.toLowerCase().trim().replaceAll("\\s+", " ");
        return String.format("%s:%s", normalizedFront, normalizedBack);
    }
}

