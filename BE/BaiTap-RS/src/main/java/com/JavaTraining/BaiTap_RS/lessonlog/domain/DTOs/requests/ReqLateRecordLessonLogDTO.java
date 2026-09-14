package com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record ReqLateRecordLessonLogDTO(@NotNull @Positive Long timetableEntryId,
        @NotNull java.time.LocalDate lessonDate, @NotBlank String reason,
        @NotBlank String title, String content, @NotBlank String completionStatus,
        @NotBlank String grade, @NotNull @PositiveOrZero Integer presentCount,
        @NotNull @PositiveOrZero Integer absentCount, String comments,
        String absentStudentNotes, String homework) { }
