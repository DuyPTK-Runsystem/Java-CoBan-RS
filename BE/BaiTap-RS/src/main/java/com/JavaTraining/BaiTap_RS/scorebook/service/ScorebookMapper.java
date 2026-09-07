package com.JavaTraining.BaiTap_RS.scorebook.service;

import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResAssessmentColumnDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScorebookDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResSkillWeightConfigDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.SkillWeightConfig;
import org.springframework.stereotype.Component;

@Component
public class ScorebookMapper {

    public ResAssessmentColumnDTO toColumnResponse(AssessmentColumn column) {
        return new ResAssessmentColumnDTO(
                column.getId(),
                column.getScorebookId(),
                column.getAssessmentType(),
                column.getColumnNo(),
                column.getColumnName(),
                column.getWeightFactor(),
                column.isRequired(),
                column.getStatus());
    }

    public ResSkillWeightConfigDTO toWeightResponse(SkillWeightConfig config) {
        if (config == null) {
            return null;
        }
        return new ResSkillWeightConfigDTO(
                config.getId(),
                config.getScorebookId(),
                config.getKtttWeightPercent(),
                config.getKtdkWeightPercent(),
                config.getKtckWeightPercent(),
                config.getConfiguredBy(),
                config.getConfiguredAt(),
                config.getLockedBy(),
                config.getLockedAt());
    }

    public ResScorebookDTO toScorebookResponse(
            Scorebook scorebook,
            java.util.List<ResAssessmentColumnDTO> columns,
            ResSkillWeightConfigDTO weightConfig) {
        return new ResScorebookDTO(
                scorebook.getId(),
                scorebook.getClassSubjectId(),
                scorebook.getStatus(),
                scorebook.getPublishedAt(),
                scorebook.getPublishedBy(),
                scorebook.getClosedAt(),
                columns,
                weightConfig);
    }
}
