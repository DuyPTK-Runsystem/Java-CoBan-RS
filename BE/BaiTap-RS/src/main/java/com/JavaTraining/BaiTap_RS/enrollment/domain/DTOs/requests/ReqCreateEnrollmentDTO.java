package com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReqCreateEnrollmentDTO(
        @NotNull @Positive Long studentId,
        @NotNull @Positive Long academicYearId,
        @NotNull @Positive Long classId,
        LocalDateTime enrolledAt) {
}
