package com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response;

import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceExceptionStatus;

public record ResAttendanceExceptionDTO(
        Long attendanceRecordId,
        Long sessionId,
        Long studentId,
        String studentCode,
        String studentName,
        AttendanceExceptionStatus status,
        String note,
        Long recordedBy,
        LocalDateTime recordedAt,
        Long updatedBy,
        LocalDateTime updatedAt) {
}
