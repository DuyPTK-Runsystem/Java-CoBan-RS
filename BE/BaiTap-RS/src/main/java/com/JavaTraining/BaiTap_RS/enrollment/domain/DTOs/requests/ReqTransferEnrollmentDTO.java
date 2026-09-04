package com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReqTransferEnrollmentDTO(
        @NotNull @Positive Long targetClassId,
        @NotNull @PastOrPresent(message = "Ngày hiệu lực không được ở tương lai") LocalDateTime effectiveAt,
        @Size(max = 500) String reason) {
}
