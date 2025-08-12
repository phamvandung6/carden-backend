package com.loopy.carden.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "jobs", indexes = {
    @Index(name = "idx_job_user", columnList = "user_id"),
    @Index(name = "idx_job_status", columnList = "status"),
    @Index(name = "idx_job_type", columnList = "job_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Job extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Column(name = "job_type", nullable = false)
    private String jobType; // AI_DECK_GENERATION, DATA_IMPORT, DATA_EXPORT, etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.PENDING;

    @Type(JsonType.class)
    @Column(name = "job_data", columnDefinition = "jsonb")
    private Map<String, Object> jobData; // Input parameters

    @Type(JsonType.class)
    @Column(name = "result_data", columnDefinition = "jsonb")
    private Map<String, Object> resultData; // Output results

    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;

    @Column(name = "status_message")
    private String statusMessage;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    public enum JobStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    // Helper methods
    public boolean isCompleted() {
        return status == JobStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == JobStatus.FAILED;
    }

    public boolean isRunning() {
        return status == JobStatus.RUNNING;
    }

    public boolean canRetry() {
        return retryCount < maxRetries && (status == JobStatus.FAILED);
    }

    public void markAsStarted() {
        this.status = JobStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    public void markAsCompleted() {
        this.status = JobStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.progressPercentage = 100;
    }

    public void markAsFailed(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }
}

