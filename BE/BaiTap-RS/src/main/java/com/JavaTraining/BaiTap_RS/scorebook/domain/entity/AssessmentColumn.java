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
        name = "assessment_column",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_assessment_column_position",
                columnNames = {"scorebook_id", "assessment_type", "column_no"}))
public class AssessmentColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assessment_column_id", nullable = false)
    private Long id;

    @Column(name = "scorebook_id", nullable = false)
    private Long scorebookId;

    @Enumerated(EnumType.STRING)
    @Column(name = "assessment_type", nullable = false, length = 20)
    private AssessmentType assessmentType;

    @Column(name = "column_no", nullable = false)
    private Integer columnNo;

    @Column(name = "column_name", length = 100)
    private String columnName;

    @Column(name = "weight_factor", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightFactor;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AssessmentColumnStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public AssessmentColumn(
            Long scorebookId,
            AssessmentType assessmentType,
            Integer columnNo,
            String columnName,
            BigDecimal weightFactor,
            boolean required) {
        this.scorebookId = scorebookId;
        this.assessmentType = assessmentType;
        this.columnNo = columnNo;
        this.columnName = columnName;
        this.weightFactor = weightFactor;
        this.required = required;
        this.status = AssessmentColumnStatus.ACTIVE;
    }

    public void deactivate() {
        status = AssessmentColumnStatus.INACTIVE;
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
