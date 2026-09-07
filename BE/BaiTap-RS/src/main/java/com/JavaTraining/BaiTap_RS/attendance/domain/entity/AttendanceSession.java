package com.JavaTraining.BaiTap_RS.attendance.domain.entity;

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
        name = "attendance_session",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_attendance_session_class_date_period",
                columnNames = {"class_id", "attendance_date", "session_period"}))
public class AttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id", nullable = false)
    private Long id;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "semester_id", nullable = false)
    private Long semesterId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_period", nullable = false, length = 20)
    private AttendanceSessionPeriod sessionPeriod;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AttendanceSession(
            Long classId,
            Long semesterId,
            LocalDate attendanceDate,
            AttendanceSessionPeriod sessionPeriod,
            Long createdBy) {
        this.classId = classId;
        this.semesterId = semesterId;
        this.attendanceDate = attendanceDate;
        this.sessionPeriod = sessionPeriod;
        this.createdBy = createdBy;
    }

    @PrePersist
    /* default */ void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
