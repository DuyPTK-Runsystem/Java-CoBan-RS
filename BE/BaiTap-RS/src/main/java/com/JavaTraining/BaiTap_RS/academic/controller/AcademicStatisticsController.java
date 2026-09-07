package com.JavaTraining.BaiTap_RS.academic.controller;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResAcademicYearStatisticsDTO;
import com.JavaTraining.BaiTap_RS.academic.service.AcademicStatisticsService;
import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2/academic/years")
@SuppressWarnings("PMD.GuardLogStatement")
public class AcademicStatisticsController {

    private final AcademicStatisticsService academicStatisticsService;

    public AcademicStatisticsController(AcademicStatisticsService academicStatisticsService) {
        this.academicStatisticsService = academicStatisticsService;
    }

    @GetMapping("/{academicYearId}/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')")
    @ApiMessage("Lấy thống kê sĩ số và cảnh báo học vụ")
    public ResAcademicYearStatisticsDTO getAcademicYearStatistics(
            @PathVariable("academicYearId") @Positive Long academicYearId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                AcademicStatisticsController.class,
                "AcademicStatisticsController.getAcademicYearStatistics");
        return academicStatisticsService.getAcademicYearStatistics(academicYearId);
    }
}
