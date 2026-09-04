package com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSessionPeriod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReqCreateAttendanceSessionDTO(
        @NotNull @Positive Long classId,
        @NotNull @Positive Long semesterId,
        @NotNull LocalDate attendanceDate,
        @NotNull AttendanceSessionPeriod sessionPeriod) {
}
