package com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests;
import jakarta.validation.constraints.NotNull; import jakarta.validation.constraints.PositiveOrZero;
public record ReqSubmitLessonLogDTO(@NotNull @PositiveOrZero Long expectedVersion) { }
