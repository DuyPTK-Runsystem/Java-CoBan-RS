package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response;

import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResCapacityWarningDTO;

public record ResClassStatisticDTO(
                Long classId,
                String classCode,
                String className,
                Long gradeLevelId,
                Integer capacity,
                long activeStudentCount,
                Double gradeAverage,
                ResCapacityWarningDTO warning) {
}
