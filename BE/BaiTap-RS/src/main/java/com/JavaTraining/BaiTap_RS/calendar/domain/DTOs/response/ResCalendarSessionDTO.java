package com.JavaTraining.BaiTap_RS.calendar.domain.DTOs.response;

import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSessionPeriod;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSessionStatus;

public record ResCalendarSessionDTO(
        Long id,
        CalendarSessionPeriod sessionPeriod,
        CalendarSessionStatus sessionStatus,
        String reason,
        Long configuredBy,
        LocalDateTime configuredAt,
        Long updatedBy,
        LocalDateTime updatedAt) {
}
