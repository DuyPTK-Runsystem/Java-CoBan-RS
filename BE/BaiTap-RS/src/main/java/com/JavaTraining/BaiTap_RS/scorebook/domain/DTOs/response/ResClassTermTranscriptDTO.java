package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response;

import java.math.BigDecimal;
import java.util.List;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationStatus;

public record ResClassTermTranscriptDTO(
                Long classId,
                String classCode,
                String className,
                Long academicYearId,
                Long semesterId,
                List<ClassTermStudentRowDTO> students) {

        public record ClassTermStudentRowDTO(
                        Long studentId,
                        String studentCode,
                        String fullName,
                        CalculationStatus calculationStatus,
                        BigDecimal dtbhk,
                        List<ResStudentTermTranscriptDTO.ResTermSubjectResultDTO> subjects) {
        }
}
