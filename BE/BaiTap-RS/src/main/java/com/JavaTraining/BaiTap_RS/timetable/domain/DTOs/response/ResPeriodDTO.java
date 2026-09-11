package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response;

import java.time.LocalTime;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.SessionType;

public record ResPeriodDTO(
        Long periodId,
        Long calendarId,
        Integer dayOfWeek,
        SessionType session,
        Integer periodIndex,
        String name,
        LocalTime startTime,
        LocalTime endTime
) {}
