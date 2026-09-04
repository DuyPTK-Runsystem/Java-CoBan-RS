package com.JavaTraining.BaiTap_RS.attendance.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceRecord;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSession;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarDay;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSession;
import org.springframework.stereotype.Component;

@Component
public class AttendanceHistoryDataReader {

    private final AttendanceHistoryCalendarReader calendarReader;
    private final AttendanceHistorySessionReader sessionReader;

    public AttendanceHistoryDataReader(
            AttendanceHistoryCalendarReader calendarReader,
            AttendanceHistorySessionReader sessionReader) {
        this.calendarReader = calendarReader;
        this.sessionReader = sessionReader;
    }

    public Map<Long, SchoolClass> loadClasses(List<Long> classIds) {
        return calendarReader.loadClasses(classIds);
    }

    public List<CalendarDay> findCalendarDays(Long semesterId, LocalDate from, LocalDate to) {
        return calendarReader.findCalendarDays(semesterId, from, to);
    }

    public List<CalendarSession> findCalendarSessions(Long calendarDayId) {
        return calendarReader.findCalendarSessions(calendarDayId);
    }

    public Map<String, AttendanceSession> findSessionsMap(Long classId, LocalDate from, LocalDate to) {
        return sessionReader.findSessionsMap(classId, from, to);
    }

    public Optional<AttendanceRecord> findRecord(Long sessionId, Long studentId) {
        return sessionReader.findRecord(sessionId, studentId);
    }
}
