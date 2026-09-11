package com.JavaTraining.BaiTap_RS.timetable.domain.entity;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "timetable_period", uniqueConstraints = @UniqueConstraint(name = "uk_timetable_period", columnNames = {
        "calendar_id", "day_of_week", "session", "period_index" }))
public class TimetablePeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "period_id", nullable = false)
    private Long id;

    @Column(name = "calendar_id", nullable = false)
    private Long calendarId;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Enumerated(EnumType.STRING)
    @Column(name = "session", nullable = false, length = 20)
    private SessionType session;

    @Column(name = "period_index", nullable = false)
    private Integer periodIndex;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    public TimetablePeriod(
            Long calendarId,
            Integer dayOfWeek,
            SessionType session,
            Integer periodIndex,
            String name,
            LocalTime startTime,
            LocalTime endTime) {
        this.calendarId = calendarId;
        this.dayOfWeek = dayOfWeek;
        this.session = session;
        this.periodIndex = periodIndex;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
