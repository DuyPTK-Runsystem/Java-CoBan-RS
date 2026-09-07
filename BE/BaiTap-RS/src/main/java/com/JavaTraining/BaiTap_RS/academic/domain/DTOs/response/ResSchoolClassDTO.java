package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;

public record ResSchoolClassDTO(
        Long id,
        Long academicYearId,
        Long gradeLevelId,
        String classCode,
        String className,
        Integer capacity,
        SchoolClassStatus status) {
}
