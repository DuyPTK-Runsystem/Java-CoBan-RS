package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response;

import java.time.LocalDateTime;
import java.util.List;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;

public record ResScorebookDTO(
        Long id,
        Long classSubjectId,
        ScorebookStatus status,
        LocalDateTime publishedAt,
        Long publishedBy,
        LocalDateTime closedAt,
        List<ResAssessmentColumnDTO> columns,
        ResSkillWeightConfigDTO skillWeightConfig) {
}
