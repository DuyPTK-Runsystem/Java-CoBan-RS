package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response;

import java.util.List;

public record ResCalendarDTO(
        Long calendarId,
        Long semesterId,
        Long version,
        List<ResPeriodDTO> periods,
        List<ResClosedDateDTO> closedDates
) {}
