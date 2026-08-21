package com.JavaTraining.BaiTap_RS.enrollment.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// BR-ENROLL-001: một học sinh chỉ có một hồ sơ enrollment trong một năm học.
@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(
        name = "student_year_enrollment",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_enrollment_student_year",
                columnNames = {"student_id", "academic_year_id"}))
public class StudentYearEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enrollment_id", nullable = false)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "current_class_id", nullable = false)
    private Long currentClassId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EnrollmentStatus status;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public StudentYearEnrollment(
            Long studentId,
            Long academicYearId,
            Long currentClassId,
            EnrollmentStatus status,
            LocalDateTime enrolledAt) {
        this.studentId = studentId;
        this.academicYearId = academicYearId;
        this.currentClassId = currentClassId;
        this.status = status;
        this.enrolledAt = enrolledAt;
    }

    @jakarta.persistence.PrePersist
    /* default */ void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @jakarta.persistence.PreUpdate
    /* default */ void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
