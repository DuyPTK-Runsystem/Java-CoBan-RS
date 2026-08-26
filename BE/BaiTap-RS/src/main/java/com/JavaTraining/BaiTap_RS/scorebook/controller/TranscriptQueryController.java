package com.JavaTraining.BaiTap_RS.scorebook.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentAnnualTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentTermTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.service.TranscriptQueryService;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2/transcripts/students")
public class TranscriptQueryController {

    private static final String STUDENT_ROLE = "hasRole('STUDENT')";
    private static final String STAFF_ROLES = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')";
    private final TranscriptQueryService transcriptQueryService;

    public TranscriptQueryController(TranscriptQueryService transcriptQueryService) {
        this.transcriptQueryService = transcriptQueryService;
    }

    @GetMapping("/me/semesters/{semesterId}")
    @ApiMessage("Xem bảng điểm học kỳ của bản thân")
    @PreAuthorize(STUDENT_ROLE)
    public ResStudentTermTranscriptDTO getMyTerm(@PathVariable @Positive Long semesterId) {
        return transcriptQueryService.getMyTermTranscript(semesterId);
    }

    @GetMapping("/me/academic-years/{academicYearId}")
    @ApiMessage("Xem bảng điểm năm học của bản thân")
    @PreAuthorize(STUDENT_ROLE)
    public ResStudentAnnualTranscriptDTO getMyAnnual(@PathVariable @Positive Long academicYearId) {
        return transcriptQueryService.getMyAnnualTranscript(academicYearId);
    }

    @GetMapping("/{studentId}/semesters/{semesterId}")
    @ApiMessage("Xem bảng điểm học kỳ của học sinh")
    @PreAuthorize(STAFF_ROLES)
    public ResStudentTermTranscriptDTO getTerm(@PathVariable @Positive Long studentId,
            @PathVariable @Positive Long semesterId) {
        return transcriptQueryService.getTermTranscript(studentId, semesterId);
    }

    @GetMapping("/{studentId}/academic-years/{academicYearId}")
    @ApiMessage("Xem bảng điểm năm học của học sinh")
    @PreAuthorize(STAFF_ROLES)
    public ResStudentAnnualTranscriptDTO getAnnual(@PathVariable @Positive Long studentId,
            @PathVariable @Positive Long academicYearId) {
        return transcriptQueryService.getAnnualTranscript(studentId, academicYearId);
    }
}
