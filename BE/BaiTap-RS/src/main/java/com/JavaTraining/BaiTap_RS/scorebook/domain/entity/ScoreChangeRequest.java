package com.JavaTraining.BaiTap_RS.scorebook.domain.entity;

import java.math.BigDecimal;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "score_change_request")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuppressWarnings({ "PMD.TooManyFields", "PMD.NullAssignment" })
public class ScoreChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id", nullable = false)
    private Long id;

    @Column(name = "assessment_column_id", nullable = false)
    private Long assessmentColumnId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_score_id")
    private Long studentScoreId;

    @Enumerated(EnumType.STRING)
    @Column(name = "before_status", nullable = false, length = 20)
    private ScoreSnapshotStatus beforeStatus;

    @Column(name = "before_value", precision = 3, scale = 1)
    private BigDecimal beforeValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposed_status", nullable = false, length = 20)
    private ScoreStatus proposedStatus;

    @Column(name = "proposed_value", precision = 3, scale = 1)
    private BigDecimal proposedValue;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScoreChangeRequestStatus status;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "pending_request_key", unique = true, length = 100)
    private String pendingRequestKey;

    public ScoreChangeRequest(
            Long assessmentColumnId,
            Long studentId,
            Long studentScoreId,
            ScoreSnapshotStatus beforeStatus,
            BigDecimal beforeValue,
            ScoreStatus proposedStatus,
            BigDecimal proposedValue,
            String reason,
            Long requestedBy,
            LocalDateTime requestedAt) {
        this.assessmentColumnId = assessmentColumnId;
        this.studentId = studentId;
        this.studentScoreId = studentScoreId;
        this.beforeStatus = beforeStatus;
        this.beforeValue = beforeValue;
        this.proposedStatus = proposedStatus;
        this.proposedValue = proposedValue;
        this.reason = reason;
        this.requestedBy = requestedBy;
        this.requestedAt = requestedAt;
        this.status = ScoreChangeRequestStatus.PENDING;
        this.pendingRequestKey = assessmentColumnId + ":" + studentId;
    }

    public void apply(Long reviewerId, LocalDateTime reviewedTime) {
        this.status = ScoreChangeRequestStatus.APPLIED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = reviewedTime;
        this.appliedAt = reviewedTime;
        this.pendingRequestKey = null;
    }

    public void reject(Long reviewerId, LocalDateTime reviewedTime, String reason) {
        this.status = ScoreChangeRequestStatus.REJECTED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = reviewedTime;
        this.rejectionReason = reason;
        this.pendingRequestKey = null;
    }

    public void cancel() {
        this.status = ScoreChangeRequestStatus.CANCELLED;
        this.pendingRequestKey = null;
    }

    @PrePersist
    /* default */ void prePersist() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ScoreChangeRequestStatus.PENDING;
        }
        if (status == ScoreChangeRequestStatus.PENDING && pendingRequestKey == null) {
            pendingRequestKey = assessmentColumnId + ":" + studentId;
        }
    }
}
