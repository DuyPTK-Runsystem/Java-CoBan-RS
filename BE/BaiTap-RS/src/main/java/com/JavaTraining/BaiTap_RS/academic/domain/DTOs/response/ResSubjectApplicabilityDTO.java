package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicabilityStatus;

public record ResSubjectApplicabilityDTO(
        Long id,
        Long subjectId,
        Long semesterId,
        ApplicationScope scopeType,
        Long gradeLevelId,
        Long classId,
        SubjectApplicabilityStatus status) {
}
