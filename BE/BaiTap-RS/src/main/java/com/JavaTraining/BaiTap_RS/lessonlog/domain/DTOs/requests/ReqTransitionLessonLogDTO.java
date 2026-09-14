package com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ReqTransitionLessonLogDTO(@NotNull @PositiveOrZero Long expectedVersion, String comment,
        @NotBlank String reason) { }
