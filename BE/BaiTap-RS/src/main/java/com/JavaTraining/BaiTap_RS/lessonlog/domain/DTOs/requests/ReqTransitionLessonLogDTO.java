package com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ReqTransitionLessonLogDTO(@NotNull @PositiveOrZero Long expectedVersion, String comment,
        @NotBlank String reason, String title, String content, String completionStatus, String grade,
        @PositiveOrZero Integer presentCount, @PositiveOrZero Integer absentCount, String comments,
        String absentStudentNotes, String homework) {
    public ReqTransitionLessonLogDTO(Long expectedVersion, String comment, String reason) {
        this(expectedVersion, comment, reason, null, null, null, null, null, null, null, null, null);
    }
}
