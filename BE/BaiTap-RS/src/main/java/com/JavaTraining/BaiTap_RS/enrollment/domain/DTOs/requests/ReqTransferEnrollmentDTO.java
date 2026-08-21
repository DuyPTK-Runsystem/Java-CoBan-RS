package com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReqTransferEnrollmentDTO(
        @NotNull @Positive Long targetClassId,
        @NotNull LocalDateTime effectiveAt,
        @Size(max = 500) String reason) {
}
