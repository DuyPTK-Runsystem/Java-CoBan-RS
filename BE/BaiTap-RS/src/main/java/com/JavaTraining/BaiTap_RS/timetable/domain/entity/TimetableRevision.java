package com.JavaTraining.BaiTap_RS.timetable.domain.entity;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "timetable_revision", uniqueConstraints = @UniqueConstraint(name = "uk_ttr_revision", columnNames = {
        "timetable_id", "revision_number" }))
public class TimetableRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "revision_id", nullable = false)
    private Long id;

    @Column(name = "timetable_id", nullable = false)
    private Long timetableId;

    @Column(name = "semester_id", nullable = false)
    private Long semesterId;

    @Column(name = "revision_number", nullable = false)
    private Integer revisionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TimetableRevisionStatus status;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "policy_id")
    private Long policyId;

    @Column(name = "validation_fingerprint", length = 255)
    private String validationFingerprint;

    @Column(name = "blocking_count", nullable = false)
    private Integer blockingCount;

    @Column(name = "warning_count", nullable = false)
    private Integer warningCount;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public TimetableRevision(
            Long timetableId,
            Long semesterId,
            Integer revisionNumber,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            Long policyId) {
        this.timetableId = timetableId;
        this.semesterId = semesterId;
        this.revisionNumber = revisionNumber;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.policyId = policyId;
        this.status = TimetableRevisionStatus.DRAFT;
        this.blockingCount = 0;
        this.warningCount = 0;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = TimetableRevisionStatus.DRAFT;
        }
        if (this.blockingCount == null) {
            this.blockingCount = 0;
        }
        if (this.warningCount == null) {
            this.warningCount = 0;
        }
        if (this.version == null) {
            this.version = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
