package com.JavaTraining.BaiTap_RS.calendar.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.calendar.domain.DTOs.response.ResCalendarDayDTO;
import com.JavaTraining.BaiTap_RS.calendar.domain.DTOs.response.ResCalendarSessionDTO;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarDay;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSession;
import org.springframework.stereotype.Component;

@Component
public class CalendarMapper {

    public ResCalendarDayDTO toDayResponse(CalendarDay day, List<CalendarSession> sessions) {
        return new ResCalendarDayDTO(
                day.getId(),
                day.getAcademicYearId(),
                day.getSemesterId(),
                day.getCalendarDate(),
                day.getDayType(),
                day.getReason(),
                day.getConfiguredBy(),
                day.getConfiguredAt(),
                day.getUpdatedBy(),
                day.getUpdatedAt(),
                sessions.stream().map(this::toSessionResponse).toList());
    }

    private ResCalendarSessionDTO toSessionResponse(CalendarSession session) {
        return new ResCalendarSessionDTO(
                session.getId(),
                session.getSessionPeriod(),
                session.getSessionStatus(),
                session.getReason(),
                session.getConfiguredBy(),
                session.getConfiguredAt(),
                session.getUpdatedBy(),
                session.getUpdatedAt());
    }
}
