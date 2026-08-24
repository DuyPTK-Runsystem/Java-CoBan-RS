package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReqCreateAssessmentColumnDTO(
        @NotNull AssessmentType assessmentType,
        @NotNull @Positive Integer columnNo,
        @Size(max = 100) String columnName) {
}
