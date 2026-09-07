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
@Table(name = "student_term_transcript", uniqueConstraints = {
        @UniqueConstraint(name = "uk_term_transcript_annual_semester", columnNames = { "annual_transcript_id",
                "semester_id" })
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentTermTranscript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "term_transcript_id")
    private Long id;

    @Column(name = "annual_transcript_id", nullable = false)
    private Long annualTranscriptId;

    @Column(name = "semester_id", nullable = false)
    private Long semesterId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_status", nullable = false, length = 20)
    private CalculationStatus calculationStatus;

    @Column(name = "source_version", nullable = false)
    private Long sourceVersion;

    @Column(name = "calculated_version")
    private Long calculatedVersion;

    @Column(name = "dtbhk", precision = 3, scale = 1)
    private BigDecimal dtbhk;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public StudentTermTranscript(Long annualTranscriptId, Long semesterId, Long studentId) {
        this.annualTranscriptId = annualTranscriptId;
        this.semesterId = semesterId;
        this.studentId = studentId;
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
