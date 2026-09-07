package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReqCreateScorebookDTO(
        @NotNull @Positive Long classSubjectId) {
}
