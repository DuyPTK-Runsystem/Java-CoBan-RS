package com.JavaTraining.BaiTap_RS.attendance.domain.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(
        name = "attendance_record",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_attendance_record_session_student",
                columnNames = {"session_id", "student_id"}))
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_record_id", nullable = false)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceExceptionStatus status;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "recorded_by", nullable = false)
    private Long recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public AttendanceRecord(
            Long sessionId,
            Long studentId,
            AttendanceExceptionStatus status,
            String note,
            Long recordedBy) {
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.status = status;
        this.note = note;
        this.recordedBy = recordedBy;
    }

    public void update(AttendanceExceptionStatus status, String note, Long updatedBy) {
        this.status = status;
        this.note = note;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    /* default */ void onCreate() {
        recordedAt = LocalDateTime.now();
    }
}
