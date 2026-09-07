package com.JavaTraining.BaiTap_RS.scorebook.domain.entity;

import java.math.BigDecimal;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "retake_exam", uniqueConstraints = {
        @UniqueConstraint(name = "uk_retake_student_year_subject", columnNames = {
                "student_id", "academic_year_id", "subject_id"
        })
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RetakeExam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "retake_id")
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "pre_retake_score", precision = 3, scale = 1, nullable = false)
    private BigDecimal preRetakeScore;

    @Column(name = "retake_score", precision = 3, scale = 1)
    private BigDecimal retakeScore;

    @Column(name = "exam_date")
    private LocalDate examDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RetakeExamStatus status;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public RetakeExam(
            Long studentId,
            Long academicYearId,
            Long subjectId,
            BigDecimal preRetakeScore,
            RetakeExamStatus status) {
        this.studentId = studentId;
        this.academicYearId = academicYearId;
        this.subjectId = subjectId;
        this.preRetakeScore = preRetakeScore;
        this.status = status;
    }

    @PrePersist
    /* default */ void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    /* default */ void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
