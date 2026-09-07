package com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response;

public record ResCapacityWarningDTO(
        Long classId,
        Long academicYearId,
        Long gradeLevelId,
        long activeStudentCount,
        double gradeAverage,
        String message) {
}
