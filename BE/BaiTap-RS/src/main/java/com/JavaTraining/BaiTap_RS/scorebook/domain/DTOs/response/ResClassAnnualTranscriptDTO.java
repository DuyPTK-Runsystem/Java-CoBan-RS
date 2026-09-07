package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response;

import java.math.BigDecimal;
import java.util.List;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationResultSource;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationStatus;

public record ResClassAnnualTranscriptDTO(
                Long classId,
                String classCode,
                String className,
                Long academicYearId,
                List<ClassAnnualStudentRowDTO> students) {

        public record ClassAnnualStudentRowDTO(
                        Long studentId,
                        String studentCode,
                        String fullName,
                        CalculationStatus calculationStatus,
                        BigDecimal regularDtbcn,
                        BigDecimal finalDtbcn,
                        CalculationResultSource resultSource,
                        List<ResStudentAnnualTranscriptDTO.ResAnnualSubjectResultDTO> subjects) {
        }
}
