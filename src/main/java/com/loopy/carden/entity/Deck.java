package com.loopy.carden.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
@Table(name = "decks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted = false")
public class Deck extends BaseEntity {

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false)
    private String title;

    @Size(max = 1000)
    private String description;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility = Visibility.PRIVATE;

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_level")
    private CEFRLevel cefrLevel;

    @Size(max = 10)
    @Column(name = "source_language")
    private String sourceLanguage = "en";

    @Size(max = 10)
    @Column(name = "target_language")
    private String targetLanguage = "vi";

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> tags;

    @Column(name = "is_system_deck")
    private boolean isSystemDeck = false;

    @Column(name = "is_public")
    private boolean isPublic = false;

    @Column(name = "download_count")
    private Long downloadCount = 0L;

    @Column(name = "like_count")
    private Long likeCount = 0L;

    @Column(name = "card_count")
    private Integer cardCount = 0;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // Audit timestamp

    // Relationships
    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private Set<Card> cards;

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<StudyState> studyStates;

    public enum Visibility {
        PRIVATE, PUBLIC, UNLISTED
    }

    public enum CEFRLevel {
        A1, A2, B1, B2, C1, C2
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
}

