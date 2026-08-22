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
        name = "semester",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_semester_year_code",
                columnNames = {"academic_year_id", "code"}))
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "semester_id", nullable = false)
    private Long id;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "automatic_lock_at")
    private LocalDateTime automaticLockAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SemesterStatus status;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "locked_by")
    private Long lockedBy;

    @Column(name = "lock_reason", length = 500)
    private String lockReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Semester(
            Long academicYearId,
            String code,
            String name,
            Integer displayOrder,
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime automaticLockAt,
            SemesterStatus status) {
        this.academicYearId = academicYearId;
        this.code = code;
        this.name = name;
        this.displayOrder = displayOrder;
        this.startDate = startDate;
        this.endDate = endDate;
        this.automaticLockAt = automaticLockAt;
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
