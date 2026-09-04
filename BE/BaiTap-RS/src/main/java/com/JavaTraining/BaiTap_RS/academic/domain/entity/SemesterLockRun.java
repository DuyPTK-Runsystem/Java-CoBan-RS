package com.JavaTraining.BaiTap_RS.academic.domain.entity;

import java.time.LocalDate;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(name = "semester_lock_run")
public class SemesterLockRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "run_id", nullable = false)
    private Long id;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "batch_execution_id")
    private Long batchExecutionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SemesterLockRunStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    public SemesterLockRun(LocalDate businessDate, Long batchExecutionId, SemesterLockRunStatus status) {
        this.businessDate = businessDate;
        this.batchExecutionId = batchExecutionId;
        this.status = status;
        this.startedAt = LocalDateTime.now();
    }

    public void markSucceeded() {
        this.status = SemesterLockRunStatus.SUCCEEDED;
        this.finishedAt = LocalDateTime.now();
    }

    public void markFailed(String error) {
        this.status = SemesterLockRunStatus.FAILED;
        this.finishedAt = LocalDateTime.now();
        this.lastError = error;
    }

    @PrePersist
    /* default */ void onCreate() {
        if (this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        }
    }
}
