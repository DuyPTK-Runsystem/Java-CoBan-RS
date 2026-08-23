package com.JavaTraining.BaiTap_RS.calendar.domain.DTOs.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarDayType;

public record ResCalendarDayDTO(
        Long id,
        Long academicYearId,
        Long semesterId,
        LocalDate calendarDate,
        CalendarDayType dayType,
        String reason,
        Long configuredBy,
        LocalDateTime configuredAt,
        Long updatedBy,
        LocalDateTime updatedAt,
        List<ResCalendarSessionDTO> sessions) {
}
