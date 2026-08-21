package com.JavaTraining.BaiTap_RS.academic.domain.entity;

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

@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
// FR-CLASS-001 and BR-CLASS-002: class identity is unique within an academic year.
@Table(
        name = "school_class",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_school_class_year_code",
                columnNames = {"academic_year_id", "class_code"}))
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id", nullable = false)
    private Long id;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "grade_level_id", nullable = false)
    private Long gradeLevelId;

    @Column(name = "class_code", nullable = false, length = 30)
    private String classCode;

    @Column(name = "class_name", length = 100)
    private String className;

    @Column(name = "capacity")
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SchoolClassStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public SchoolClass(
            Long academicYearId,
            Long gradeLevelId,
            String classCode,
            String className,
            Integer capacity,
            SchoolClassStatus status) {
        this.academicYearId = academicYearId;
        this.gradeLevelId = gradeLevelId;
        this.classCode = classCode;
        this.className = className;
        this.capacity = capacity;
        this.status = status;
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
