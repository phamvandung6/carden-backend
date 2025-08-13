package com.loopy.carden.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "review_sessions", indexes = {
    @Index(name = "idx_review_session_user", columnList = "user_id"),
    @Index(name = "idx_review_session_date", columnList = "session_date"),
    @Index(name = "idx_review_session_deck", columnList = "deck_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSession extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id")
    private Deck deck;

    @Column(name = "session_date", nullable = false)
    private LocalDateTime sessionDate;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "cards_studied")
    private Integer cardsStudied = 0;

    @Column(name = "cards_correct")
    private Integer cardsCorrect = 0;

    @Column(name = "new_cards")
    private Integer newCards = 0;

    @Column(name = "review_cards")
    private Integer reviewCards = 0;

    @Column(name = "relearning_cards")
    private Integer relearningCards = 0;

    @Column(name = "accuracy_rate")
    private Double accuracyRate = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "study_mode", nullable = false)
    private StudyMode studyMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_status")
    private SessionStatus sessionStatus = SessionStatus.IN_PROGRESS;

    @Type(JsonType.class)
    @Column(name = "session_stats", columnDefinition = "jsonb")
    private SessionStats sessionStats;

    public enum StudyMode {
        FLIP,           // Self-graded flashcard mode
        TYPE_ANSWER,    // Type the answer mode
        MULTIPLE_CHOICE // Multiple choice mode
    }

    public enum SessionStatus {
        IN_PROGRESS,
        COMPLETED,
        ABANDONED
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionStats {
        private Integer averageResponseTime; // in milliseconds
        private List<Integer> scoreDistribution; // [again, hard, good, easy] counts
        private Integer totalTimeSpent; // in seconds
        private Integer pauseCount;
        private Double difficultyRating; // User's subjective difficulty rating
    }

    // Helper methods
    public void updateAccuracyRate() {
        if (cardsStudied > 0) {
            this.accuracyRate = (double) cardsCorrect / cardsStudied * 100.0;
        }
    }

    public boolean isCompleted() {
        return sessionStatus == SessionStatus.COMPLETED;
    }

    public boolean isInProgress() {
        return sessionStatus == SessionStatus.IN_PROGRESS;
    }
}

