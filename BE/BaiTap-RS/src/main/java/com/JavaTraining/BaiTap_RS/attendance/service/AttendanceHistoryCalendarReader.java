package com.JavaTraining.BaiTap_RS.attendance.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarDay;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSession;
import com.JavaTraining.BaiTap_RS.calendar.repository.CalendarDayRepository;
import com.JavaTraining.BaiTap_RS.calendar.repository.CalendarSessionRepository;
import org.springframework.stereotype.Component;

@Component
public class AttendanceHistoryCalendarReader {

    private final SchoolClassRepository schoolClassRepository;
    private final CalendarDayRepository calendarDayRepository;
    private final CalendarSessionRepository calendarSessionRepository;

    public AttendanceHistoryCalendarReader(
            SchoolClassRepository schoolClassRepository,
            CalendarDayRepository calendarDayRepository,
            CalendarSessionRepository calendarSessionRepository) {
        this.schoolClassRepository = schoolClassRepository;
        this.calendarDayRepository = calendarDayRepository;
        this.calendarSessionRepository = calendarSessionRepository;
    }

    public Map<Long, SchoolClass> loadClasses(List<Long> classIds) {
        Map<Long, SchoolClass> result = new HashMap<>();
        schoolClassRepository.findAllById(classIds)
                .forEach(schoolClass -> result.put(schoolClass.getId(), schoolClass));
        return result;
    }

    public List<CalendarDay> findCalendarDays(Long semesterId, LocalDate from, LocalDate to) {
        return semesterId == null
                ? calendarDayRepository.findAllByCalendarDateBetweenOrderByCalendarDateAsc(from, to)
                : calendarDayRepository.findAllBySemesterIdAndCalendarDateBetweenOrderByCalendarDateAsc(
                        semesterId, from, to);
    }

    public List<CalendarSession> findCalendarSessions(Long calendarDayId) {
        return calendarSessionRepository.findAllByCalendarDayIdOrderBySessionPeriodAsc(calendarDayId);
    }
}
