package com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests;

import java.math.BigDecimal;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReqTransferTargetScoreDTO(
        @NotNull @Positive Long assessmentColumnId,
        @NotNull ScoreStatus scoreStatus,
        BigDecimal scoreValue,
        @Size(max = 500) String note,
        Long expectedVersion) {
}
