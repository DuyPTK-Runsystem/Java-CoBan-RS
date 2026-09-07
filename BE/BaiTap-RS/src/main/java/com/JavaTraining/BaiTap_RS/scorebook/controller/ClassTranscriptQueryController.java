package com.JavaTraining.BaiTap_RS.scorebook.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResClassAnnualTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResClassTermTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.service.ClassTranscriptQueryService;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2/transcripts/classes")
@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')")
@SuppressWarnings("PMD.GuardLogStatement")
public class ClassTranscriptQueryController {

    private final ClassTranscriptQueryService classTranscriptQueryService;

    public ClassTranscriptQueryController(ClassTranscriptQueryService classTranscriptQueryService) {
        this.classTranscriptQueryService = classTranscriptQueryService;
    }

    @GetMapping("/{classId}/semesters/{semesterId}")
    @ApiMessage("Xem bảng điểm học kỳ theo lớp")
    public ResClassTermTranscriptDTO getClassTermTranscript(
            @PathVariable("classId") @Positive Long classId,
            @PathVariable("semesterId") @Positive Long semesterId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ClassTranscriptQueryController.class,
                "ClassTranscriptQueryController.getClassTermTranscript");
        return classTranscriptQueryService.getClassTermTranscript(classId, semesterId);
    }

    @GetMapping("/{classId}/academic-years/{academicYearId}")
    @ApiMessage("Xem bảng điểm năm học theo lớp")
    public ResClassAnnualTranscriptDTO getClassAnnualTranscript(
            @PathVariable("classId") @Positive Long classId,
            @PathVariable("academicYearId") @Positive Long academicYearId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ClassTranscriptQueryController.class,
                "ClassTranscriptQueryController.getClassAnnualTranscript");
        return classTranscriptQueryService.getClassAnnualTranscript(classId, academicYearId);
    }
}
