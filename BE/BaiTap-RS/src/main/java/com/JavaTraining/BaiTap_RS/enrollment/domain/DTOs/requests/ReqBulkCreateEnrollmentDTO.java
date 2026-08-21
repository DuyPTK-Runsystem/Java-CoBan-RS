package com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReqBulkCreateEnrollmentDTO(
        @NotNull @Positive Long academicYearId,
        @NotNull @Positive Long classId,
        @NotEmpty List<@NotNull @Positive Long> studentIds,
        LocalDateTime enrolledAt) {
}
