package com.JavaTraining.BaiTap_RS.academic.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
// FR-GRADE-001 and BR-GRADE-001: grade metadata is unique and reusable by classes.
@Table(
        name = "grade_level",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_grade_level_code", columnNames = "code"),
            @UniqueConstraint(name = "uk_grade_level_level", columnNames = "grade_level")
        })
public class GradeLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grade_level_id", nullable = false)
    private Long id;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "grade_level", nullable = false)
    private Integer level;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "next_grade_id")
    private Long nextGradeId;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public GradeLevel(
            String code,
            String name,
            Integer gradeLevel,
            Integer displayOrder,
            Long nextGradeId,
            boolean active,
            String description) {
        this.code = code;
        this.name = name;
        this.level = gradeLevel;
        this.displayOrder = displayOrder;
        this.nextGradeId = nextGradeId;
        this.active = active;
        this.description = description;
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
