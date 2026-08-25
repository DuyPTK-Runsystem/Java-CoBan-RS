package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.LinkedHashMap;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScoreChangeRequestDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScoreChangeRequestDetailDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreChangeRequest;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import org.springframework.stereotype.Component;

@Component
public class ScoreChangeRequestMapper {

    public ResScoreChangeRequestDTO toResponse(ScoreChangeRequest request) {
        return toResponse(request, null);
    }

    public ResScoreChangeRequestDTO toResponse(ScoreChangeRequest request, Student student) {
        return new ResScoreChangeRequestDTO(
                request.getId(),
                request.getAssessmentColumnId(),
                request.getStudentId(),
                student == null ? null : student.getStudentCode(),
                student == null ? null : student.getStudentName(),
                request.getProposedStatus(),
                request.getProposedValue(),
                request.getRequestedBy(),
                request.getRequestedAt(),
                request.getStatus(),
                request.getReviewedBy(),
                request.getReviewedAt());
    }

    public ResScoreChangeRequestDetailDTO toDetail(ScoreChangeRequest request) {
        return toDetail(request, null);
    }

    public ResScoreChangeRequestDetailDTO toDetail(ScoreChangeRequest request, Student student) {
        return new ResScoreChangeRequestDetailDTO(
                request.getId(),
                request.getAssessmentColumnId(),
                request.getStudentId(),
                student == null ? null : student.getStudentCode(),
                student == null ? null : student.getStudentName(),
                request.getStudentScoreId(),
                request.getBeforeStatus(),
                request.getBeforeValue(),
                request.getProposedStatus(),
                request.getProposedValue(),
                request.getReason(),
                request.getRequestedBy(),
                request.getRequestedAt(),
                request.getStatus(),
                request.getReviewedBy(),
                request.getReviewedAt(),
                request.getRejectionReason(),
                request.getAppliedAt());
    }

    public Map<String, Object> toAuditData(ScoreChangeRequest request) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("requestId", request.getId());
        data.put("assessmentColumnId", request.getAssessmentColumnId());
        data.put("studentId", request.getStudentId());
        data.put("studentScoreId", request.getStudentScoreId());
        data.put("beforeStatus", request.getBeforeStatus());
        data.put("beforeValue", request.getBeforeValue());
        data.put("proposedStatus", request.getProposedStatus());
        data.put("proposedValue", request.getProposedValue());
        data.put("reason", request.getReason());
        data.put("requestedBy", request.getRequestedBy());
        data.put("status", request.getStatus());
        data.put("reviewedBy", request.getReviewedBy());
        data.put("rejectionReason", request.getRejectionReason());
        data.put("appliedAt", request.getAppliedAt());
        return data;
    }
}
