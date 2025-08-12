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
import java.util.Map;

@Entity
@Table(name = "reports", indexes = {
    @Index(name = "idx_report_reporter", columnList = "reporter_id"),
    @Index(name = "idx_report_status", columnList = "status"),
    @Index(name = "idx_report_type", columnList = "report_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Report extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id")
    private User reportedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_deck_id")
    private Deck reportedDeck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_card_id")
    private Card reportedCard;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.PENDING;

    @Type(JsonType.class)
    @Column(name = "additional_data", columnDefinition = "jsonb")
    private Map<String, Object> additionalData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    public enum ReportType {
        INAPPROPRIATE_CONTENT,
        COPYRIGHT_VIOLATION,
        SPAM,
        HARASSMENT,
        MISINFORMATION,
        OTHER
    }

    public enum ReportStatus {
        PENDING,
        UNDER_REVIEW,
        RESOLVED,
        DISMISSED
    }

    // Helper methods
    public boolean isPending() {
        return status == ReportStatus.PENDING;
    }

    public boolean isResolved() {
        return status == ReportStatus.RESOLVED;
    }

    public void markAsReviewed(User adminUser, String notes) {
        this.reviewedBy = adminUser;
        this.reviewedAt = LocalDateTime.now();
        this.adminNotes = notes;
        this.status = ReportStatus.UNDER_REVIEW;
    }

    public void resolve(String adminNotes) {
        this.status = ReportStatus.RESOLVED;
        this.adminNotes = adminNotes;
        this.reviewedAt = LocalDateTime.now();
    }

    public void dismiss(String adminNotes) {
        this.status = ReportStatus.DISMISSED;
        this.adminNotes = adminNotes;
        this.reviewedAt = LocalDateTime.now();
    }
}

