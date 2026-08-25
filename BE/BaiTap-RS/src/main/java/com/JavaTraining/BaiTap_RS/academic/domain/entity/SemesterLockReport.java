package com.JavaTraining.BaiTap_RS.academic.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(
        name = "semester_lock_report",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lock_report_run_sem_chk",
                columnNames = {"run_id", "semester_id", "checkpoint_code"}))
public class SemesterLockReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id", nullable = false)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "semester_id", nullable = false)
    private Long semesterId;

    @Column(name = "checkpoint_code", nullable = false, length = 30)
    private String checkpointCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_status", nullable = false, length = 20)
    private SemesterLockReportStatus reportStatus;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;

    @Column(name = "scope_type", nullable = false, length = 30)
    private String scopeType;

    @Column(name = "summary_payload", nullable = false, columnDefinition = "TEXT")
    private String summaryPayload;

    @Column(name = "failure_reason", length = 2000)
    private String failureReason;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public SemesterLockReport(
            Long runId,
            Long semesterId,
            String checkpointCode,
            SemesterLockReportStatus reportStatus,
            LocalDateTime evaluatedAt,
            String scopeType,
            String summaryPayload,
            String failureReason,
            String correlationId) {
        this.runId = runId;
        this.semesterId = semesterId;
        this.checkpointCode = checkpointCode;
        this.reportStatus = reportStatus;
        this.evaluatedAt = evaluatedAt != null ? evaluatedAt : LocalDateTime.now();
        this.scopeType = scopeType != null ? scopeType : "SEMESTER";
        this.summaryPayload = summaryPayload;
        this.failureReason = failureReason;
        this.correlationId = correlationId;
    }

    @PrePersist
    /* default */ void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (evaluatedAt == null) {
            evaluatedAt = now;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    /* default */ void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
