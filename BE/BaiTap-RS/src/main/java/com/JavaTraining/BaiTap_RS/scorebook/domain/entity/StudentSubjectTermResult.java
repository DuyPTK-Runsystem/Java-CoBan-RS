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
@Table(name = "student_subject_term_result", uniqueConstraints = {
        @UniqueConstraint(name = "uk_subject_term_result", columnNames = { "term_transcript_id", "subject_id" })
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentSubjectTermResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "term_result_id")
    private Long id;

    @Column(name = "term_transcript_id", nullable = false)
    private Long termTranscriptId;

    @Column(name = "class_subject_id", nullable = false)
    private Long classSubjectId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 20)
    private SubjectType subjectType;

    @Column(name = "dtbmh", precision = 3, scale = 1)
    private BigDecimal dtbmh;

    @Column(name = "skill_score", precision = 3, scale = 1)
    private BigDecimal skillScore;

    @Column(name = "calculated_version")
    private Long calculatedVersion;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public StudentSubjectTermResult(
            Long termTranscriptId,
            Long classSubjectId,
            Long subjectId,
            SubjectType subjectType) {
        this.termTranscriptId = termTranscriptId;
        this.classSubjectId = classSubjectId;
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
