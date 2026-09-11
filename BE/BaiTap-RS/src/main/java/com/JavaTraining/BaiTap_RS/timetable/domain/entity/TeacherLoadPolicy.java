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
@Table(name = "teacher_load_policy", uniqueConstraints = @UniqueConstraint(name = "uk_tlp_version", columnNames = "version"))
public class TeacherLoadPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id", nullable = false)
    private Long id;

    @Column(name = "version", nullable = false, length = 50)
    private String version;

    @Column(name = "source", nullable = false, length = 255)
    private String source;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "base_periods", nullable = false)
    private Integer basePeriods;

    @Column(name = "homeroom_reduction", nullable = false)
    private Integer homeroomReduction;

    @Column(name = "nursing_reduction", nullable = false)
    private Integer nursingReduction;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TeacherLoadPolicyStatus status;

    @Version
    @Column(name = "version_lock", nullable = false)
    private Long versionLock = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public TeacherLoadPolicy(
            String version,
            String source,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            Integer basePeriods,
            Integer homeroomReduction,
            Integer nursingReduction) {
        this.version = version;
        this.source = source;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.basePeriods = basePeriods != null ? basePeriods : 19;
        this.homeroomReduction = homeroomReduction != null ? homeroomReduction : 4;
        this.nursingReduction = nursingReduction != null ? nursingReduction : 3;
        this.status = TeacherLoadPolicyStatus.DRAFT;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = TeacherLoadPolicyStatus.DRAFT;
        }
        if (this.versionLock == null) {
            this.versionLock = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
