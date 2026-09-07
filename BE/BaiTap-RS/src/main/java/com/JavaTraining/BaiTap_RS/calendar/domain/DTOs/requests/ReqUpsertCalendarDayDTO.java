package com.JavaTraining.BaiTap_RS.calendar.domain.DTOs.requests;

import java.util.List;

import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarDayType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReqUpsertCalendarDayDTO(
        @NotNull @Positive Long academicYearId,
        @NotNull @Positive Long semesterId,
        @NotNull CalendarDayType dayType,
        @Size(max = 500) String reason,
        @NotNull @Size(max = 2) List<@Valid ReqCalendarSessionDTO> sessions) {
}
