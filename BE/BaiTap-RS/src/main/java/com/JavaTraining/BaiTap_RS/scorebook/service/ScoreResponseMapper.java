package com.JavaTraining.BaiTap_RS.scorebook.service;

import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentScoreDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi Entity StudentScore sang Response DTO.
 */
@Component
public class ScoreResponseMapper {

    public ResStudentScoreDTO toResponse(StudentScore score) {
        return toResponse(score, null);
    }

    public ResStudentScoreDTO toResponse(StudentScore score, Student student) {
        return new ResStudentScoreDTO(
                score.getId(),
                score.getAssessmentColumnId(),
                score.getStudentId(),
                student == null ? null : student.getStudentCode(),
                student == null ? null : student.getStudentName(),
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
