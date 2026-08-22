package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReqCreateClassSubjectDTO(
        @NotNull @Positive Long classId,
        @NotNull @Positive Long subjectId,
        @NotNull @Positive Long semesterId,
        @NotNull ClassSubjectStatus status) {
}
