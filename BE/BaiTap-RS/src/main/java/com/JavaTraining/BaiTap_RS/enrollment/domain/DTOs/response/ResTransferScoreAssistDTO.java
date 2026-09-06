package com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response;

import java.util.List;

public record ResTransferScoreAssistDTO(
        Long enrollmentId,
        Long studentId,
        String studentCode,
        String studentName,
        Long academicYearId,
        Long semesterId,
        boolean hasExistingScores,
        ResTransferClassDTO sourceClass,
        ResTransferClassDTO targetClass,
        List<ResTransferScoreSubjectDTO> subjects,
        List<String> warnings) {
}
