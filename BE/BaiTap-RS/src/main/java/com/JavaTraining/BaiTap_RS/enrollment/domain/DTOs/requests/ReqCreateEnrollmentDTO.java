package com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests;

import java.time.LocalDateTime;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReqCreateEnrollmentDTO(
        @Positive Long studentId,
        String studentCode,
        @NotNull @Positive Long academicYearId,
        @NotNull @Positive Long classId,
        LocalDateTime enrolledAt) {

    @AssertTrue(message = "Phải cung cấp studentId hoặc studentCode")
    public boolean isStudentIdentifierProvided() {
        return studentId != null || (studentCode != null && !studentCode.isBlank());
    }
}
