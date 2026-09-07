package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response;

public record ResGradeStatisticDTO(
                Long gradeLevelId,
                long activeClassCount,
                long activeStudentCount) {
}
