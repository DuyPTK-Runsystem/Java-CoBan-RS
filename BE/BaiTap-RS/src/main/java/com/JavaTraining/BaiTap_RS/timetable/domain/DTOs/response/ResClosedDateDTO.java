package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response;

import java.time.LocalDate;

public record ResClosedDateDTO(
        Long id,
        Long calendarId,
        LocalDate closedDate,
        String reason
) {}
