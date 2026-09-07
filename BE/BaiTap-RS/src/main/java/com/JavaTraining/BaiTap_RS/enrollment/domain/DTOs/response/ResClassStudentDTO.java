package com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response;

public record ResClassStudentDTO(
        Long studentId,
        String studentCode,
        String studentName,
        Long enrollmentId) {
}
