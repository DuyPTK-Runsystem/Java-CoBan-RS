package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentType;

public record ResScoreGridColumnDTO(
        Long columnId,
        AssessmentType assessmentType,
        Integer columnNo,
        String columnName
) {
}
