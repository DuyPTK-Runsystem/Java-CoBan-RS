package com.JavaTraining.BaiTap_RS.timetable.domain.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "timetable_closed_date", uniqueConstraints = @UniqueConstraint(name = "uk_timetable_closed_date", columnNames = {
        "calendar_id", "closed_date" }))
public class TimetableClosedDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "calendar_id", nullable = false)
    private Long calendarId;

    @Column(name = "closed_date", nullable = false)
    private LocalDate closedDate;

    @Column(name = "reason", length = 255)
    private String reason;

    public TimetableClosedDate(Long calendarId, LocalDate closedDate, String reason) {
        this.calendarId = calendarId;
        this.closedDate = closedDate;
        this.reason = reason;
    }
}
