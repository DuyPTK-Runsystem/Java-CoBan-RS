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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(name = "subject_applicability")
public class SubjectApplicability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subject_applicability_id", nullable = false)
    private Long id;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "semester_id", nullable = false)
    private Long semesterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 20)
    private ApplicationScope scopeType;

    @Column(name = "grade_level_id")
    private Long gradeLevelId;

    @Column(name = "class_id")
    private Long classId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubjectApplicabilityStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public SubjectApplicability(
            Long subjectId,
            Long semesterId,
            ApplicationScope scopeType,
            Long gradeLevelId,
            Long classId,
            SubjectApplicabilityStatus status) {
        this.subjectId = subjectId;
        this.semesterId = semesterId;
        this.scopeType = scopeType;
        this.gradeLevelId = gradeLevelId;
        this.classId = classId;
        this.status = status;
    }

    @PrePersist
    /* default */ void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    /* default */ void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
