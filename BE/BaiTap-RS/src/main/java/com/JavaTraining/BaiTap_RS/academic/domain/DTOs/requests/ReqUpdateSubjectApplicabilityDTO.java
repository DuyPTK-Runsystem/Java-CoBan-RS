package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicabilityStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReqUpdateSubjectApplicabilityDTO(
        @NotNull @Positive Long semesterId,
        @NotNull ApplicationScope scopeType,
        @Positive Long gradeLevelId,
        @Positive Long classId,
        @NotNull SubjectApplicabilityStatus status) {
}
