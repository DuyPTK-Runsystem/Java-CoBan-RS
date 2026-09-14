package com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ReqPolicyDTO(@NotNull @PositiveOrZero Long expectedVersion,
        @NotNull LocalDate effectiveFrom, @NotBlank String timezone,
        @NotBlank String deadlineMode, Integer editWindowHours,
        Boolean requireHomeroomReview, @NotBlank String rubric, @NotBlank String reason) { }
