package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests;

import java.time.LocalTime;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.SessionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReqPeriodDefinitionDTO(
                @NotNull(message = "dayOfWeek không được để trống") @Min(1) @Max(7) Integer dayOfWeek,

                @NotNull(message = "session không được để trống") SessionType session,

                @NotNull(message = "periodIndex không được để trống") @Min(1) @Max(4) Integer periodIndex,

                @NotBlank(message = "Tên tiết không được để trống") String name,

                @NotNull(message = "startTime không được để trống") LocalTime startTime,

                @NotNull(message = "endTime không được để trống") LocalTime endTime) {
}
