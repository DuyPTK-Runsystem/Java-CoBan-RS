package com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReqBulkCreateEnrollmentDTO(
        @NotNull @Positive Long academicYearId,
        @NotNull @Positive Long classId,
        List<@NotNull @Positive Long> studentIds,
        List<@NotBlank String> studentCodes,
        LocalDateTime enrolledAt) {

    @AssertTrue(message = "Phải cung cấp studentIds hoặc studentCodes")
    public boolean isStudentIdentifierProvided() {
        boolean hasIds = studentIds != null && !studentIds.isEmpty();
        boolean hasCodes = studentCodes != null && !studentCodes.isEmpty();
        return hasIds || hasCodes;
    }
}
