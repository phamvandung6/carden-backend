package com.loopy.carden.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_states", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "card_id"}),
       indexes = {
           @Index(name = "idx_study_state_user_card", columnList = "user_id, card_id"),
           @Index(name = "idx_study_state_due_date", columnList = "due_date"),
           @Index(name = "idx_study_state_user_due", columnList = "user_id, due_date")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudyState extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    @Column(name = "repetition_count", nullable = false)
    private Integer repetitionCount = 0;

    @DecimalMin("1.3")
    @DecimalMax("3.0")
    @Column(name = "ease_factor", nullable = false)
    private Double easeFactor = 2.5;

    @Column(name = "interval_days", nullable = false)
    private Integer intervalDays = 1;

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_state", nullable = false)
    private CardState cardState = CardState.NEW;

    @Column(name = "last_review_date")
    private LocalDateTime lastReviewDate;

    @Column(name = "last_score")
    private Integer lastScore; // 0-3 scale

    @Column(name = "total_reviews")
    private Integer totalReviews = 0;

    @Column(name = "correct_reviews")
    private Integer correctReviews = 0;

    @Column(name = "accuracy_rate")
    private Double accuracyRate = 0.0;

    @Column(name = "consecutive_failures")
    private Integer consecutiveFailures = 0;

    @Column(name = "current_learning_step")
    private Integer currentLearningStep = 0;

    @Column(name = "is_leech")
    private Boolean isLeech = false;

    @Column(name = "graduated_at")
    private LocalDateTime graduatedAt;

    public enum CardState {
        NEW,        // Card never studied
        LEARNING,   // Card being learned (failed cards)
        REVIEW,     // Card in normal review cycle
        RELEARNING  // Card failed from review back to learning
    }

    // Helper methods
    public boolean isDue() {
        return dueDate.isBefore(LocalDateTime.now()) || dueDate.isEqual(LocalDateTime.now());
    }

    public void updateAccuracyRate() {
        if (totalReviews > 0) {
            this.accuracyRate = (double) correctReviews / totalReviews * 100.0;
        }
    }

    public boolean isNew() {
        return cardState == CardState.NEW;
    }

    public boolean isLearning() {
        return cardState == CardState.LEARNING || cardState == CardState.RELEARNING;
    }

    public boolean isInReview() {
        return cardState == CardState.REVIEW;
    }
}

