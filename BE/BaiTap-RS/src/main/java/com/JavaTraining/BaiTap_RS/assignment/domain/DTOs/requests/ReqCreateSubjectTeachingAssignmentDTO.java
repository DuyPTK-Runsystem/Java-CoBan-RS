package com.JavaTraining.BaiTap_RS.assignment.domain.DTOs.requests;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReqCreateSubjectTeachingAssignmentDTO(
        @NotNull @Positive Long teacherId,
        @NotNull LocalDate validFrom,
        LocalDate validTo) {
}
