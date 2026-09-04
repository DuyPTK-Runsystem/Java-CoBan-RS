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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "student_annual_transcript", uniqueConstraints = {
        @UniqueConstraint(name = "uk_annual_transcript_student_year", columnNames = { "student_id",
                "academic_year_id" })
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentAnnualTranscript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "annual_transcript_id")
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_status", nullable = false, length = 20)
    private CalculationStatus calculationStatus;

    @Column(name = "source_version", nullable = false)
    private Long sourceVersion;

    @Column(name = "calculated_version")
    private Long calculatedVersion;

    @Column(name = "regular_dtbcn", precision = 3, scale = 1)
    private BigDecimal regularDtbcn;

    @Column(name = "final_dtbcn", precision = 3, scale = 1)
    private BigDecimal finalDtbcn;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_source", length = 20)
    private CalculationResultSource resultSource;

    @Column(name = "last_calculation_task_id")
    private Long lastCalculationTaskId;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public StudentAnnualTranscript(Long studentId, Long academicYearId) {
        this.studentId = studentId;
        this.academicYearId = academicYearId;
        this.calculationStatus = CalculationStatus.IN_PROGRESS;
        this.sourceVersion = 0L;
    }

    public void incrementSourceVersion() {
        this.sourceVersion++;
        this.calculationStatus = CalculationStatus.IN_PROGRESS;
    }

    public void markInProgress() {
        this.calculationStatus = CalculationStatus.IN_PROGRESS;
    }

    @PrePersist
    /* default */ void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    /* default */ void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
