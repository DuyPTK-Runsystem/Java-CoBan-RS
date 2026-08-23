package com.JavaTraining.BaiTap_RS.calendar.service;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSessionPeriod;
import org.springframework.stereotype.Service;

@Service
public class CalendarValidityService {

    private final CalendarService calendarService;

    public CalendarValidityService(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    public void assertScheduled(Long semesterId, LocalDate attendanceDate, AttendanceSessionPeriod period) {
        calendarService.assertScheduled(semesterId, attendanceDate, period.name());
    }
}
