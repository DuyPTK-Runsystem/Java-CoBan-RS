package com.JavaTraining.BaiTap_RS.scorebook.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
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
@Table(name = "student_subject_annual_result", uniqueConstraints = {
        @UniqueConstraint(name = "uk_subject_annual_result", columnNames = { "annual_transcript_id", "subject_id" })
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentSubjectAnnualResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "annual_subject_result_id")
    private Long id;

    @Column(name = "annual_transcript_id", nullable = false)
    private Long annualTranscriptId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "hk1_term_result_id")
    private Long hk1TermResultId;

    @Column(name = "hk2_term_result_id")
    private Long hk2TermResultId;

    @Column(name = "retake_id")
    private Long retakeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 20)
    private SubjectType subjectType;

    @Column(name = "regular_dtbmh_cn", precision = 3, scale = 1)
    private BigDecimal regularDtbmhCn;

    @Column(name = "official_dtbmh_cn", precision = 3, scale = 1)
    private BigDecimal officialDtbmhCn;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_source", length = 20)
    private CalculationResultSource calculationSource;

    @Column(name = "calculated_version")
    private Long calculatedVersion;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public StudentSubjectAnnualResult(Long annualTranscriptId, Long subjectId, SubjectType subjectType) {
        this.annualTranscriptId = annualTranscriptId;
        this.subjectId = subjectId;
        this.subjectType = subjectType;
    }

    @PrePersist
    /* default */ void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    /* default */ void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
