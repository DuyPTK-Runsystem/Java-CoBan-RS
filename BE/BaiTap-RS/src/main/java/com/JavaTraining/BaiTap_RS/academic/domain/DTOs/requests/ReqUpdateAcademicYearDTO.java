package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReqUpdateAcademicYearDTO(
        @NotBlank @Size(max = 20) String code,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull AcademicYearStatus status,
        @Size(max = 500) String notes) {
}
