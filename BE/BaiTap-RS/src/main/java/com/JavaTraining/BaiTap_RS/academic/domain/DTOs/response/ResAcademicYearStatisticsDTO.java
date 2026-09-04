package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response;

import java.util.List;

public record ResAcademicYearStatisticsDTO(
                Long academicYearId,
                List<ResGradeStatisticDTO> gradeStatistics,
                List<ResClassStatisticDTO> classStatistics,
                int totalWarnings) {
}
