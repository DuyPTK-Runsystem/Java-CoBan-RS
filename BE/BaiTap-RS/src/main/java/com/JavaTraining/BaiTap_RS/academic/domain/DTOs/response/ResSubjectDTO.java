package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;

public record ResSubjectDTO(
        Long id,
        String code,
        String name,
        SubjectType subjectType,
        ApplicationScope applicationScope,
        SubjectStatus status) {
}
