package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response;

public record ResGradeLevelDTO(
        Long id,
        String code,
        String name,
        Integer gradeLevel,
        Integer displayOrder,
        Long nextGradeId,
        boolean active,
        String description) {
}
