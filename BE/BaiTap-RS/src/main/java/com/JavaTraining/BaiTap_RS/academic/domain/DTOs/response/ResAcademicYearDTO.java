package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus;

public record ResAcademicYearDTO(
        Long id,
        String code,
        LocalDate startDate,
        LocalDate endDate,
        AcademicYearStatus status,
        String notes) {
}
