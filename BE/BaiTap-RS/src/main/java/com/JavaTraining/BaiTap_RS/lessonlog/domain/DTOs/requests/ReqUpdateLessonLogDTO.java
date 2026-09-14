package com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests;
import jakarta.validation.constraints.*;
public record ReqUpdateLessonLogDTO(@NotNull @Positive Long expectedVersion,String title,String content,String completionStatus,String grade,@PositiveOrZero Integer presentCount,@PositiveOrZero Integer absentCount,String comments,String absentStudentNotes,String homework) {}
