package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response;

import java.util.Map;

public record ResScoreGridStudentRowDTO(
        Long studentId,
        String studentCode,
        String studentName,
        Map<Long, ResStudentScoreDTO> scores
) {
}
