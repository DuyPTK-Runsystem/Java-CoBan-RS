package com.JavaTraining.BaiTap_RS.attendance.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSessionPeriod;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarDay;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSession;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSessionStatus;
import com.JavaTraining.BaiTap_RS.calendar.repository.CalendarDayRepository;
import com.JavaTraining.BaiTap_RS.calendar.repository.CalendarSessionRepository;
import org.springframework.stereotype.Component;

@Component
public class ClassAttendanceSummaryCalendarReader {

    private final CalendarDayRepository calendarDayRepository;
    private final CalendarSessionRepository calendarSessionRepository;

    public ClassAttendanceSummaryCalendarReader(
            CalendarDayRepository calendarDayRepository,
            CalendarSessionRepository calendarSessionRepository) {
        this.calendarDayRepository = calendarDayRepository;
        this.calendarSessionRepository = calendarSessionRepository;
    }

    public List<ClassAttendanceSummaryCollector.SessionSlot> collectScheduledSlots(
            Long semesterId,
            LocalDate from,
            LocalDate to) {
        List<CalendarDay> calendarDays = calendarDayRepository
                .findAllBySemesterIdAndCalendarDateBetweenOrderByCalendarDateAsc(semesterId, from, to);
        if (calendarDays.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> dayIds = calendarDays.stream().map(CalendarDay::getId).toList();
        Map<Long, LocalDate> dayDateMap = calendarDays.stream().collect(
                Collectors.toMap(CalendarDay::getId, CalendarDay::getCalendarDate));

        List<CalendarSession> sessions = calendarSessionRepository.findAllByCalendarDayIdIn(dayIds);
        List<ClassAttendanceSummaryCollector.SessionSlot> slots = new ArrayList<>();

        for (CalendarSession session : sessions) {
            if (session.getSessionStatus() == CalendarSessionStatus.SCHEDULED) {
                LocalDate date = dayDateMap.get(session.getCalendarDayId());
                AttendanceSessionPeriod period = AttendanceSessionPeriod.valueOf(
                        session.getSessionPeriod().name());
                slots.add(new ClassAttendanceSummaryCollector.SessionSlot(date, period));
            }
        }

        slots.sort(Comparator.comparing(ClassAttendanceSummaryCollector.SessionSlot::date)
                .thenComparing(s -> s.period().name()));
        return slots;
    }
}
