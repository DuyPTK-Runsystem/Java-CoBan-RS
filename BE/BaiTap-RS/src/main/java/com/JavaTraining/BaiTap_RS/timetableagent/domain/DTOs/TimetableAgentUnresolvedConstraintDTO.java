package com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TimetableAgentUnresolvedConstraintDTO(
        @NotBlank String code,
        @NotNull String field,
        @NotBlank String message) {
}
