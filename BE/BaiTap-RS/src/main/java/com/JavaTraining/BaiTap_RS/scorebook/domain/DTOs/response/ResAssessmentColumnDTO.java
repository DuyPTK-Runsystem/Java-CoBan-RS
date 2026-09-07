package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response;

import java.math.BigDecimal;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumnStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentType;

public record ResAssessmentColumnDTO(
        Long id,
        Long scorebookId,
        AssessmentType assessmentType,
        Integer columnNo,
        String columnName,
        BigDecimal weightFactor,
        boolean required,
        AssessmentColumnStatus status) {
}
