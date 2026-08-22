package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReqCreateSemesterDTO(
        @NotNull @Positive Long academicYearId,
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 100) String name,
        @NotNull @Positive Integer displayOrder,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        LocalDateTime automaticLockAt,
        @NotNull SemesterStatus status) {
}
