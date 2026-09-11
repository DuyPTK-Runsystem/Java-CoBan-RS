package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record ReqCreateRevisionDTO(
        @NotNull(message = "expectedVersion không được để trống")
        Long expectedVersion,

        @NotNull(message = "Ngày áp dụng cho revision mới không được để trống")
        LocalDate effectiveFrom
) {}
