package com.JavaTraining.BaiTap_RS.scorebook.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentAnnualTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentTermTranscriptDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResTranscriptCalculationStatusDTO;
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
@SuppressWarnings("PMD.GuardLogStatement")
public class TranscriptQueryController {

    private static final String STUDENT_ROLE = "hasRole('STUDENT')";
    private static final String STAFF_ROLES = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')";
    private static final String SEMESTER_ID = "semesterId";
    private static final String ACADEMIC_YEAR_ID = "academicYearId";
    private static final String STUDENT_ID = "studentId";
    private final TranscriptQueryService transcriptQueryService;

    public TranscriptQueryController(TranscriptQueryService transcriptQueryService) {
        this.transcriptQueryService = transcriptQueryService;
    }

    @GetMapping("/me/semesters/{semesterId}")
    @ApiMessage("Xem bảng điểm học kỳ của bản thân")
    @PreAuthorize(STUDENT_ROLE)
    public ResStudentTermTranscriptDTO getMyTerm(
            @PathVariable(SEMESTER_ID) @Positive Long semesterId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        TranscriptQueryController.class,
                        "TranscriptQueryController.getMyTerm");
        return transcriptQueryService.getMyTermTranscript(semesterId);
    }

    @GetMapping("/me/academic-years/{academicYearId}")
    @ApiMessage("Xem bảng điểm năm học của bản thân")
    @PreAuthorize(STUDENT_ROLE)
    public ResStudentAnnualTranscriptDTO getMyAnnual(
            @PathVariable(ACADEMIC_YEAR_ID) @Positive Long academicYearId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        TranscriptQueryController.class,
                        "TranscriptQueryController.getMyAnnual");
        return transcriptQueryService.getMyAnnualTranscript(academicYearId);
    }

    @GetMapping("/me/semesters/{semesterId}/status")
    @ApiMessage("Xem trạng thái tính bảng điểm học kỳ của bản thân")
    @PreAuthorize(STUDENT_ROLE)
    public ResTranscriptCalculationStatusDTO getMyTermStatus(
            @PathVariable(SEMESTER_ID) @Positive Long semesterId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        TranscriptQueryController.class,
                        "TranscriptQueryController.getMyTermStatus");
        return transcriptQueryService.getMyTermCalculationStatus(semesterId);
    }

    @GetMapping("/me/academic-years/{academicYearId}/status")
    @ApiMessage("Xem trạng thái tính bảng điểm năm học của bản thân")
    @PreAuthorize(STUDENT_ROLE)
    public ResTranscriptCalculationStatusDTO getMyAnnualStatus(
            @PathVariable(ACADEMIC_YEAR_ID) @Positive Long academicYearId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        TranscriptQueryController.class,
                        "TranscriptQueryController.getMyAnnualStatus");
        return transcriptQueryService.getMyAnnualCalculationStatus(academicYearId);
    }

    @GetMapping("/{studentId}/semesters/{semesterId}")
    @ApiMessage("Xem bảng điểm học kỳ của học sinh")
    @PreAuthorize(STAFF_ROLES)
    public ResStudentTermTranscriptDTO getTerm(
            @PathVariable(STUDENT_ID) @Positive Long studentId,
            @PathVariable(SEMESTER_ID) @Positive Long semesterId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        TranscriptQueryController.class,
                        "TranscriptQueryController.getTerm");
        return transcriptQueryService.getTermTranscript(studentId, semesterId);
    }

    @GetMapping("/{studentId}/academic-years/{academicYearId}")
    @ApiMessage("Xem bảng điểm năm học của học sinh")
    @PreAuthorize(STAFF_ROLES)
    public ResStudentAnnualTranscriptDTO getAnnual(
            @PathVariable(STUDENT_ID) @Positive Long studentId,
            @PathVariable(ACADEMIC_YEAR_ID) @Positive Long academicYearId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        TranscriptQueryController.class,
                        "TranscriptQueryController.getAnnual");
        return transcriptQueryService.getAnnualTranscript(studentId, academicYearId);
    }

    @GetMapping("/{studentId}/semesters/{semesterId}/status")
    @ApiMessage("Xem trạng thái tính bảng điểm học kỳ của học sinh")
    @PreAuthorize(STAFF_ROLES)
    public ResTranscriptCalculationStatusDTO getTermStatus(
            @PathVariable(STUDENT_ID) @Positive Long studentId,
            @PathVariable(SEMESTER_ID) @Positive Long semesterId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        TranscriptQueryController.class,
                        "TranscriptQueryController.getTermStatus");
        return transcriptQueryService.getTermCalculationStatus(studentId, semesterId);
    }

    @GetMapping("/{studentId}/academic-years/{academicYearId}/status")
    @ApiMessage("Xem trạng thái tính bảng điểm năm học của học sinh")
    @PreAuthorize(STAFF_ROLES)
    public ResTranscriptCalculationStatusDTO getAnnualStatus(
            @PathVariable(STUDENT_ID) @Positive Long studentId,
            @PathVariable(ACADEMIC_YEAR_ID) @Positive Long academicYearId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        TranscriptQueryController.class,
                        "TranscriptQueryController.getAnnualStatus");
        return transcriptQueryService.getAnnualCalculationStatus(studentId, academicYearId);
    }
}
