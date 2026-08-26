package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReqCreateRetakeExamDTO(
        @NotNull(message = "ID học sinh không được để trống")
        @Positive(message = "ID học sinh phải là số dương")
        Long studentId,

        @NotNull(message = "ID năm học không được để trống")
        @Positive(message = "ID năm học phải là số dương")
        Long academicYearId,

        @NotNull(message = "ID môn học không được để trống")
        @Positive(message = "ID môn học phải là số dương")
        Long subjectId,

        LocalDate examDate,

        @DecimalMin(value = "0.0", message = "Điểm thi lại phải từ 0.0 đến 10.0")
        @DecimalMax(value = "10.0", message = "Điểm thi lại phải từ 0.0 đến 10.0")
        @Digits(
                integer = 2,
                fraction = 1,
                message = "Điểm thi lại chỉ được có tối đa 1 chữ số thập phân")
        BigDecimal retakeScore,

        @Size(max = 1000, message = "Ghi chú không quá 1000 ký tự")
        String note) {
}
