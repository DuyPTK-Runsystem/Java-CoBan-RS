package com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response;

import java.time.LocalDateTime;

public record ResAttendanceStudentDTO(
        Long studentId,
        String studentCode,
        String studentName,
        Long attendanceRecordId,
        String status,
        String note,
        Long recordedBy,
        LocalDateTime recordedAt,
        Long updatedBy,
        LocalDateTime updatedAt) {
}
