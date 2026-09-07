package com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response;

import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;

public record ResEnrollmentDTO(
        Long id,
        Long studentId,
        String studentCode,
        String studentName,
        Long academicYearId,
        Long currentClassId,
        String currentClassCode,
        EnrollmentStatus status,
        LocalDateTime enrolledAt,
        LocalDateTime completedAt) {
}
