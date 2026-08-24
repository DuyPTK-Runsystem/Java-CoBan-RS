package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ResSkillWeightConfigDTO(
        Long id,
        Long scorebookId,
        BigDecimal ktttWeightPercent,
        BigDecimal ktdkWeightPercent,
        BigDecimal ktckWeightPercent,
        Long configuredBy,
        LocalDateTime configuredAt,
        Long lockedBy,
        LocalDateTime lockedAt) {
}
