package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import org.springframework.stereotype.Component;

/**
 * NFR-AUDITABILITY-003: Tạo snapshot before/after cho audit log điểm.
 */
@Component
public class ScoreAuditDataMapper {

    public Map<String, Object> toSnapshot(StudentScore score) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("scoreId", score.getId());
        map.put("assessmentColumnId", score.getAssessmentColumnId());
        map.put("studentId", score.getStudentId());
        map.put("scoreStatus", score.getScoreStatus().name());
        map.put("scoreValue", score.getScoreValue());
        map.put("note", score.getNote());
        map.put("enteredBy", score.getEnteredBy());
        map.put("enteredAt", score.getEnteredAt() != null ? score.getEnteredAt().toString() : null);
        map.put("updatedBy", score.getUpdatedBy());
        map.put("updatedAt", score.getUpdatedAt() != null ? score.getUpdatedAt().toString() : null);
        map.put("version", score.getVersion());
        return map;
    }

    public Map<String, Object> toCreateSnapshot(
            Long columnId, Long studentId, ScoreStatus status, BigDecimal value, String note) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("assessmentColumnId", columnId);
        map.put("studentId", studentId);
        map.put("scoreStatus", status.name());
        map.put("scoreValue", value);
        map.put("note", note);
        return map;
    }
}
