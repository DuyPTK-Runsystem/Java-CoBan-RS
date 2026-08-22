package com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSessionPeriod;

public record ResAttendanceSessionDTO(
        Long sessionId,
        Long classId,
        Long semesterId,
        LocalDate attendanceDate,
        AttendanceSessionPeriod sessionPeriod,
        Long createdBy,
        LocalDateTime createdAt) {
}
