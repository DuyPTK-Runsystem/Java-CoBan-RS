package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ReqSaveCalendarDTO(
        @NotNull(message = "Học kỳ không được để trống")
        Long semesterId,

        Long expectedVersion,

        @Valid
        List<ReqPeriodDefinitionDTO> periods,

        @Valid
        List<ReqClosedDateDTO> closedDates
) {}
