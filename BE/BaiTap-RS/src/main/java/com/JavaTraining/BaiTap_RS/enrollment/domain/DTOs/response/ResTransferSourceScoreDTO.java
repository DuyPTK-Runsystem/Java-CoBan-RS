package com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response;

import java.math.BigDecimal;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;

public record ResTransferSourceScoreDTO(
        Long assessmentColumnId,
        Long scorebookId,
        AssessmentType assessmentType,
        Integer columnNo,
        String columnName,
        ScoreStatus scoreStatus,
        BigDecimal scoreValue,
        String note,
        Long version) {
}
