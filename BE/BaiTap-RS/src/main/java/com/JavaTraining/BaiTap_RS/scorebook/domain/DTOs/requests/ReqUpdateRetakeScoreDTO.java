package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReqUpdateRetakeScoreDTO(
        @NotNull(message = "Điểm thi lại không được để trống")
        @DecimalMin(value = "0.0", message = "Điểm thi lại phải từ 0.0 đến 10.0")
        @DecimalMax(value = "10.0", message = "Điểm thi lại phải từ 0.0 đến 10.0")
        @Digits(
                integer = 2,
                fraction = 1,
                message = "Điểm thi lại chỉ được có tối đa 1 chữ số thập phân")
        BigDecimal retakeScore,

        LocalDate examDate,

        @Size(max = 1000, message = "Ghi chú không quá 1000 ký tự")
        String note) {
}
