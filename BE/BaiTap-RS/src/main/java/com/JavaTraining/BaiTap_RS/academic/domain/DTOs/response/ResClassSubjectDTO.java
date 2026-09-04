package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;

public record ResClassSubjectDTO(
        Long id,
        Long classId,
        Long subjectId,
        Long semesterId,
        ClassSubjectStatus status) {
}
