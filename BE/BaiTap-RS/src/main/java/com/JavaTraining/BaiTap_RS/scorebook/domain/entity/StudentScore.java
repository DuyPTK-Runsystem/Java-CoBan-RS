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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "student_score", uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_score_column_student", columnNames = { "assessment_column_id",
                "student_id" })
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "score_id")
    private Long id;

    @Column(name = "assessment_column_id", nullable = false)
    private Long assessmentColumnId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "score_status", nullable = false, length = 20)
    private ScoreStatus scoreStatus;

    @Column(name = "score_value", precision = 3, scale = 1)
    private BigDecimal scoreValue;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "entered_by", nullable = false)
    private Long enteredBy;

    @Column(name = "entered_at", nullable = false)
    private LocalDateTime enteredAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public StudentScore(
            Long assessmentColumnId,
            Long studentId,
            ScoreStatus scoreStatus,
            BigDecimal scoreValue,
            String note,
            Long enteredBy) {
        this.assessmentColumnId = assessmentColumnId;
        this.studentId = studentId;
        this.scoreStatus = scoreStatus;
        this.scoreValue = scoreValue;
        this.note = note;
        this.enteredBy = enteredBy;
        this.enteredAt = LocalDateTime.now();
        this.version = 1L;
    }

    public void updateScore(ScoreStatus status, BigDecimal value, String note, Long actorId) {
        this.scoreStatus = status;
        this.scoreValue = value;
        this.note = note;
        this.updatedBy = actorId;
        this.updatedAt = LocalDateTime.now();
    }

    @SuppressWarnings("PMD.NullAssignment")
    public void cancel(Long actorId) {
        this.scoreStatus = ScoreStatus.CANCELLED;
        this.scoreValue = null;
        this.updatedBy = actorId;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    /* default */ void prePersist() {
        if (this.enteredAt == null) {
            this.enteredAt = LocalDateTime.now();
        }
    }
}
