package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record ReqCreateTimetableDTO(
        @NotNull(message = "Học kỳ không được để trống")
        Long semesterId,

        @NotNull(message = "Ngày bắt đầu áp dụng không được để trống")
        LocalDate effectiveFrom,

        LocalDate effectiveTo,

        Long policyId,

        Long expectedHeadVersion
) {}
