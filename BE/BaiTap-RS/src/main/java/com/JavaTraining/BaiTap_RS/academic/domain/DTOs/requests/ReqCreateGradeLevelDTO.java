package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReqCreateGradeLevelDTO(
        @NotBlank @Size(max = 10) String code,
        @NotBlank @Size(max = 50) String name,
        @NotNull @Min(6) @Max(9) Integer gradeLevel,
        @NotNull @Positive Integer displayOrder,
        @Positive Long nextGradeId,
        boolean active,
        @Size(max = 255) String description) {
}
