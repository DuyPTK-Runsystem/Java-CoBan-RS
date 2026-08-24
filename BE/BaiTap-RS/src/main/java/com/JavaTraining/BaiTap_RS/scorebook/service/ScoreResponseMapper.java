package com.JavaTraining.BaiTap_RS.scorebook.service;

import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentScoreDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi Entity StudentScore sang Response DTO.
 */
@Component
public class ScoreResponseMapper {

    public ResStudentScoreDTO toResponse(StudentScore score) {
        return new ResStudentScoreDTO(
                score.getId(),
                score.getAssessmentColumnId(),
                score.getStudentId(),
                score.getScoreStatus(),
                score.getScoreValue(),
                score.getNote(),
                score.getEnteredBy(),
                score.getEnteredAt(),
                score.getUpdatedBy(),
                score.getUpdatedAt(),
                score.getVersion());
    }
}
