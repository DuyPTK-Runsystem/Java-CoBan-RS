package com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests;
import java.time.LocalDate; import jakarta.validation.constraints.*;
public record ReqCreateLessonLogDTO(@NotNull @Positive Long timetableEntryId,@NotNull LocalDate lessonDate,String title,String content,String completionStatus,String grade,@PositiveOrZero Integer presentCount,@PositiveOrZero Integer absentCount,String comments,String absentStudentNotes,String homework) {}
