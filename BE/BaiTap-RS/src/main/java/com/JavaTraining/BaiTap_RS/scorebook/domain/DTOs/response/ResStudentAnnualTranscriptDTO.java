package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationResultSource;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.RetakeExamStatus;

public record ResStudentAnnualTranscriptDTO(
        Long studentId,
        Long academicYearId,
        CalculationStatus calculationStatus,
        Long sourceVersion,
        Long calculatedVersion,
        LocalDateTime calculatedAt,
        BigDecimal regularDtbcn,
        BigDecimal finalDtbcn,
        CalculationResultSource resultSource,
        Long lastCalculationTaskId,
        List<ResStudentTermTranscriptDTO.ResTransferNoteDTO> transferNotes,
        List<ResAnnualSubjectResultDTO> subjects) {

    public record ResAnnualSubjectResultDTO(
            Long subjectId,
            String subjectName,
            SubjectType subjectType,
            BigDecimal hk1,
            BigDecimal hk2,
            BigDecimal regularDtbmhCn,
            BigDecimal officialDtbmhCn,
            CalculationResultSource calculationSource,
            Long calculatedVersion,
            LocalDateTime calculatedAt,
            ResRetakeDetailDTO retake) {
    }

    public record ResRetakeDetailDTO(
            Long retakeId,
            BigDecimal preRetakeScore,
            BigDecimal retakeScore,
            LocalDate examDate,
            RetakeExamStatus status,
            String note) {
    }
}
