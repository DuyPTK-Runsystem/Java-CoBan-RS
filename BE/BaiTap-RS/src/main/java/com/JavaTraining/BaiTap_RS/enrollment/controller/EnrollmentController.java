package com.JavaTraining.BaiTap_RS.enrollment.controller;

import java.util.List;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests.ReqBulkCreateEnrollmentDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests.ReqCreateEnrollmentDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests.ReqTransferEnrollmentDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResClassStudentDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResEnrollmentMutationDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResStudentEnrollmentHistoryDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response.ResUnassignedStudentDTO;
import com.JavaTraining.BaiTap_RS.enrollment.service.EnrollmentQueryService;
import com.JavaTraining.BaiTap_RS.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2")
@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')")
// FR-ENROLL-001..005 and BR-AUTH-006: teachers read enrollment, office roles mutate it.
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final EnrollmentQueryService enrollmentQueryService;

    public EnrollmentController(
            EnrollmentService enrollmentService,
            EnrollmentQueryService enrollmentQueryService) {
        this.enrollmentService = enrollmentService;
        this.enrollmentQueryService = enrollmentQueryService;
    }

    @GetMapping("/classes/{classId}/students")
    @ApiMessage("Lấy danh sách học sinh của lớp")
    public List<ResClassStudentDTO> listClassStudents(@PathVariable("classId") @Positive Long classId) {
        return enrollmentQueryService.listClassStudents(classId);
    }

    @PostMapping("/enrollments")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
    @ApiMessage("Xếp học sinh vào lớp")
    public ResponseEntity<ResEnrollmentMutationDTO> createEnrollment(
            @Valid @RequestBody ReqCreateEnrollmentDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.createEnrollment(request));
    }

    @PostMapping("/enrollments/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
    @ApiMessage("Xếp nhiều học sinh vào lớp")
    public ResEnrollmentMutationDTO createBulkEnrollment(
            @Valid @RequestBody ReqBulkCreateEnrollmentDTO request) {
        return enrollmentService.createBulkEnrollment(request);
    }

    @PostMapping("/enrollments/{enrollmentId}/transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
    @ApiMessage("Chuyển lớp cho học sinh")
    public ResEnrollmentMutationDTO transferEnrollment(
            @PathVariable("enrollmentId") @Positive Long enrollmentId,
            @Valid @RequestBody ReqTransferEnrollmentDTO request) {
        return enrollmentService.transferEnrollment(enrollmentId, request);
    }

    @GetMapping("/enrollments/unassigned")
    @ApiMessage("Lấy danh sách học sinh chưa xếp lớp")
    public List<ResUnassignedStudentDTO> listUnassignedStudents(
            @RequestParam("academicYearId") @Positive Long academicYearId) {
        return enrollmentQueryService.listUnassignedStudents(academicYearId);
    }

    @GetMapping("/students/{studentId}/enrollments")
    @ApiMessage("Lấy lịch sử lớp của học sinh")
    public List<ResStudentEnrollmentHistoryDTO> listStudentHistory(
            @PathVariable("studentId") @Positive Long studentId) {
        return enrollmentQueryService.listStudentHistory(studentId);
    }

    @GetMapping("/students/by-code/{studentCode}/enrollments")
    @ApiMessage("Lấy lịch sử lớp của học sinh theo mã")
    public List<ResStudentEnrollmentHistoryDTO> listStudentHistoryByCode(
            @PathVariable("studentCode") String studentCode) {
        return enrollmentQueryService.listStudentHistoryByCode(studentCode);
    }
}
