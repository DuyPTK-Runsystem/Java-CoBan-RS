package com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests;

import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceExceptionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReqUpsertAttendanceExceptionDTO(
        @NotNull AttendanceExceptionStatus status,
        @Size(max = 500) String note) {
}
