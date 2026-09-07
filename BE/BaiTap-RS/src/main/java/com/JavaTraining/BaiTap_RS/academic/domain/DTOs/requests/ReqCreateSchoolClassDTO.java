package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReqCreateSchoolClassDTO(
        @NotNull @Positive Long academicYearId,
        @NotNull @Positive Long gradeLevelId,
        @NotBlank @Size(max = 30) String classCode,
        @Size(max = 100) String className,
        @Positive Integer capacity,
        @NotNull SchoolClassStatus status) {
}
