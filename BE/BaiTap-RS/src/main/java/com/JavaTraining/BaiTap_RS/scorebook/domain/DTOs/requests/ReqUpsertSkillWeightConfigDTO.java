package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record ReqUpsertSkillWeightConfigDTO(
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal ktttWeightPercent,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal ktdkWeightPercent,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal ktckWeightPercent) {
}
