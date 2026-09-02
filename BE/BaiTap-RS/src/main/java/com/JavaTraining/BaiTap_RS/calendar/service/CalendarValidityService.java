package com.JavaTraining.BaiTap_RS.calendar.service;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSessionPeriod;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class CalendarValidityService {

    private final CalendarService calendarService;

    public CalendarValidityService(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    public void assertScheduled(Long semesterId, LocalDate attendanceDate, AttendanceSessionPeriod period) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalendarValidityService.class,
                "CalendarValidityService.assertScheduled",
                "checking semesterId={}, date={}, period={}", semesterId, attendanceDate, period);
        calendarService.assertScheduled(semesterId, attendanceDate, period.name());
    }

    public void ensureScheduled(Long semesterId, LocalDate attendanceDate, AttendanceSessionPeriod period) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalendarValidityService.class,
                "CalendarValidityService.ensureScheduled",
                "ensuring semesterId={}, date={}, period={}", semesterId, attendanceDate, period);
        calendarService.ensureScheduled(semesterId, attendanceDate, period.name());
    }
}
