package com.JavaTraining.BaiTap_RS.scorebook.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "calculation_task", uniqueConstraints = {
        @UniqueConstraint(name = "uk_calculation_task_idempotency", columnNames = { "idempotency_key" })
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings("PMD.TooManyFields")
public class CalculationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 30)
    private CalculationTaskType taskType;

    @Column(name = "requested_version", nullable = false)
    private Long requestedVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CalculationTaskStatus status;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts;

    @Column(name = "available_at", nullable = false)
    private LocalDateTime availableAt;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "worker_id", length = 100)
    private String workerId;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public CalculationTask(
            Long studentId,
            Long academicYearId,
            CalculationTaskType taskType,
            Long requestedVersion,
            String idempotencyKey) {
        this.studentId = studentId;
        this.academicYearId = academicYearId;
        this.taskType = taskType;
        this.requestedVersion = requestedVersion;
        this.idempotencyKey = idempotencyKey;
        this.status = CalculationTaskStatus.PENDING;
        this.attemptCount = 0;
        this.maxAttempts = 3;
        this.availableAt = LocalDateTime.now();
    }

    public void updateRequestedVersion(Long newVersion) {
        this.requestedVersion = newVersion;
        this.status = CalculationTaskStatus.PENDING;
    }

    @PrePersist
    /* default */ void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
