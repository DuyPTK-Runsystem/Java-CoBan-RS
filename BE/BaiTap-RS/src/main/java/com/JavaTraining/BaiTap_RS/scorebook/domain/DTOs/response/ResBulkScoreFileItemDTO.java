package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response;

import java.math.BigDecimal;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;

public record ResBulkScoreFileItemDTO(
        Long studentId,
        String studentCode,
        ScoreStatus scoreStatus,
        BigDecimal scoreValue,
        String note,
        Long expectedVersion) {
}
