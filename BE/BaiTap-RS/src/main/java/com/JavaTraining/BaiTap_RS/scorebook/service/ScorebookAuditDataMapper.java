package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.LinkedHashMap;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.SkillWeightConfig;
import org.springframework.stereotype.Component;

@Component
public class ScorebookAuditDataMapper {

    public Map<String, Object> scorebookData(Scorebook scorebook) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", scorebook.getId());
        data.put("classSubjectId", scorebook.getClassSubjectId());
        data.put("status", scorebook.getStatus().name());
        data.put("publishedAt", scorebook.getPublishedAt());
        data.put("publishedBy", scorebook.getPublishedBy());
        data.put("closedAt", scorebook.getClosedAt());
        return data;
    }

    public Map<String, Object> columnData(AssessmentColumn column) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", column.getId());
        data.put("scorebookId", column.getScorebookId());
        data.put("assessmentType", column.getAssessmentType().name());
        data.put("columnNo", column.getColumnNo());
        data.put("columnName", column.getColumnName());
        data.put("weightFactor", column.getWeightFactor());
        data.put("required", column.isRequired());
        data.put("status", column.getStatus().name());
        return data;
    }

    public Map<String, Object> weightData(SkillWeightConfig config) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", config.getId());
        data.put("scorebookId", config.getScorebookId());
        data.put("ktttWeightPercent", config.getKtttWeightPercent());
        data.put("ktdkWeightPercent", config.getKtdkWeightPercent());
        data.put("ktckWeightPercent", config.getKtckWeightPercent());
        data.put("configuredBy", config.getConfiguredBy());
        data.put("lockedBy", config.getLockedBy());
        data.put("lockedAt", config.getLockedAt());
        return data;
    }
}
