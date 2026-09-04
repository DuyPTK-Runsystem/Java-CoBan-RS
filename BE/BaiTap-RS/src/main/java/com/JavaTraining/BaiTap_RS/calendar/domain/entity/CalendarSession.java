package com.JavaTraining.BaiTap_RS.calendar.domain.entity;

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

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "calendar_session",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_calendar_session_day_period",
                columnNames = {"calendar_day_id", "session_period"}))
public class CalendarSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "calendar_session_id", nullable = false)
    private Long id;

    @Column(name = "calendar_day_id", nullable = false)
    private Long calendarDayId;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_period", nullable = false, length = 20)
    private CalendarSessionPeriod sessionPeriod;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_status", nullable = false, length = 20)
    private CalendarSessionStatus sessionStatus;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "configured_by", nullable = false)
    private Long configuredBy;

    @Column(name = "configured_at", nullable = false, updatable = false)
    private LocalDateTime configuredAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public CalendarSession(
            Long calendarDayId,
            CalendarSessionPeriod sessionPeriod,
            CalendarSessionStatus sessionStatus,
            String reason,
            Long configuredBy) {
        this.calendarDayId = calendarDayId;
        this.sessionPeriod = sessionPeriod;
        this.sessionStatus = sessionStatus;
        this.reason = reason;
        this.configuredBy = configuredBy;
    }

    public void update(CalendarSessionStatus sessionStatus, String reason, Long updatedBy) {
        this.sessionStatus = sessionStatus;
        this.reason = reason;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    /* default */ void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        configuredAt = now;
        updatedAt = now;
    }

    @PreUpdate
    /* default */ void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
