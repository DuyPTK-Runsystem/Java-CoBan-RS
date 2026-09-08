package com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response;

import java.math.BigDecimal;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumnStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;

public record ResTransferTargetColumnDTO(
        Long assessmentColumnId,
        Long scorebookId,
        AssessmentType assessmentType,
        Integer columnNo,
        String columnName,
        AssessmentColumnStatus status,
        String mappingKey,
        Long suggestedSourceColumnId,
        ScoreStatus existingScoreStatus,
        BigDecimal existingScoreValue,
        String existingNote,
        Long existingVersion) {
}
