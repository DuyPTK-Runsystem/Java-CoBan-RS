package com.JavaTraining.BaiTap_RS.calendar.domain.entity;

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

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "calendar_day",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_calendar_day_year_date",
                columnNames = {"academic_year_id", "calendar_date"}))
public class CalendarDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "calendar_day_id", nullable = false)
    private Long id;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "semester_id", nullable = false)
    private Long semesterId;

    @Column(name = "calendar_date", nullable = false)
    private LocalDate calendarDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false, length = 20)
    private CalendarDayType dayType;

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

    public CalendarDay(
            Long academicYearId,
            Long semesterId,
            LocalDate calendarDate,
            CalendarDayType dayType,
            String reason,
            Long configuredBy) {
        this.academicYearId = academicYearId;
        this.semesterId = semesterId;
        this.calendarDate = calendarDate;
        this.dayType = dayType;
        this.reason = reason;
        this.configuredBy = configuredBy;
    }

    public void update(CalendarDayType dayType, String reason, Long updatedBy) {
        this.dayType = dayType;
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
