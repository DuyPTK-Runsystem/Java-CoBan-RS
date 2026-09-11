package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record ReqClosedDateDTO(
                @NotNull(message = "closedDate không được để trống") LocalDate closedDate,

                String reason) {
}
