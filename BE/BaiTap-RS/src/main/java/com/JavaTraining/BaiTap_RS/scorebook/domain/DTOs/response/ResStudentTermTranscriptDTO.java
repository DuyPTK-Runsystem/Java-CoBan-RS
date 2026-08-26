package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;

public record ResStudentTermTranscriptDTO(
        Long studentId,
        Long academicYearId,
        Long semesterId,
        CalculationStatus calculationStatus,
        Long sourceVersion,
        Long calculatedVersion,
        LocalDateTime calculatedAt,
        BigDecimal dtbhk,
        List<ResTransferNoteDTO> transferNotes,
        List<ResTermSubjectResultDTO> subjects) {

    public record ResTermSubjectResultDTO(
            Long subjectId,
            String subjectName,
            SubjectType subjectType,
            BigDecimal dtbmh,
            BigDecimal skillScore,
            Long calculatedVersion,
            LocalDateTime calculatedAt,
            List<ResAssessmentColumnDTO> assessmentColumns) {
    }

    public record ResAssessmentColumnDTO(
            Long columnId,
            AssessmentType assessmentType,
            Integer columnNo,
            String columnName,
            ScoreStatus scoreStatus,
            BigDecimal scoreValue) {
    }

    public record ResTransferNoteDTO(Long fromClassId, Long toClassId, LocalDateTime effectiveAt) {
    }
}
