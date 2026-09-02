package com.JavaTraining.BaiTap_RS.academic.controller;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateSemesterDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqReopenSemesterDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateSemesterDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSemesterCompletenessDecisionDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSemesterCompletenessReportDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSemesterDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSemesterNotificationDTO;
import com.JavaTraining.BaiTap_RS.academic.service.SemesterCompletenessService;
import com.JavaTraining.BaiTap_RS.academic.service.SemesterService;
import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2/semesters")
@SuppressWarnings("PMD.GuardLogStatement")
public class SemesterController {

    private static final String OFFICE_ROLES = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')";
    private static final String SEMESTER_ID = "semesterId";

    private final SemesterService semesterService;
    private final SemesterCompletenessService completenessService;

    public SemesterController(
            SemesterService semesterService,
            SemesterCompletenessService completenessService) {
        this.semesterService = semesterService;
        this.completenessService = completenessService;
    }

    @GetMapping
    @ApiMessage("Lấy danh sách học kỳ")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER', 'STUDENT')")
    public List<ResSemesterDTO> listByAcademicYear(
            @RequestParam("academicYearId") @Positive Long academicYearId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        SemesterController.class,
                        "SemesterController.listByAcademicYear");
        return semesterService.listByAcademicYear(academicYearId);
    }

    @PostMapping
    @ApiMessage("Tạo học kỳ")
    @PreAuthorize(OFFICE_ROLES)
    public ResponseEntity<ResSemesterDTO> createSemester(@Valid @RequestBody ReqCreateSemesterDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SemesterController.class,
                "SemesterController.createSemester");
        return ResponseEntity.status(HttpStatus.CREATED).body(semesterService.createSemester(request));
    }

    @PutMapping("/{semesterId}")
    @ApiMessage("Cập nhật học kỳ")
    @PreAuthorize(OFFICE_ROLES)
    public ResSemesterDTO updateSemester(
            @PathVariable(SEMESTER_ID) @Positive Long semesterId,
            @Valid @RequestBody ReqUpdateSemesterDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        SemesterController.class,
                        "SemesterController.updateSemester");
        return semesterService.updateSemester(semesterId, request);
    }

    @PostMapping("/{semesterId}/activate")
    @ApiMessage("Kích hoạt học kỳ")
    @PreAuthorize(OFFICE_ROLES)
    public ResSemesterDTO activateSemester(@PathVariable(SEMESTER_ID) @Positive Long semesterId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SemesterController.class,
                "SemesterController.activateSemester");
        return semesterService.activateSemester(semesterId);
    }

    @PostMapping("/{semesterId}/lock")
    @ApiMessage("Khóa học kỳ")
    @PreAuthorize(OFFICE_ROLES)
    public ResSemesterDTO lockSemester(@PathVariable(SEMESTER_ID) @Positive Long semesterId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SemesterController.class,
                "SemesterController.lockSemester");
        return semesterService.lockSemester(semesterId);
    }

    @PostMapping("/{semesterId}/reopen")
    @ApiMessage("Mở lại học kỳ")
    @PreAuthorize(OFFICE_ROLES)
    public ResSemesterDTO reopenSemester(
            @PathVariable(SEMESTER_ID) @Positive Long semesterId,
            @Valid @RequestBody ReqReopenSemesterDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        SemesterController.class,
                        "SemesterController.reopenSemester");
        return semesterService.reopenSemester(semesterId, request);
    }

    @GetMapping("/{semesterId}/completeness-report")
    @ApiMessage("Xem báo cáo mức độ hoàn thành nhập điểm")
    @PreAuthorize(OFFICE_ROLES)
    public ResSemesterCompletenessReportDTO getCompletenessReport(
            @PathVariable(SEMESTER_ID) @Positive Long semesterId,
            @RequestParam(name = "checkpointCode", required = false) String checkpointCode) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        SemesterController.class,
                        "SemesterController.getCompletenessReport");
        return completenessService.getLatestReport(semesterId, checkpointCode);
    }

    @GetMapping("/{semesterId}/completeness-decision")
    @ApiMessage("Kiểm tra checkpoint dữ liệu điểm")
    @PreAuthorize(OFFICE_ROLES)
    public ResSemesterCompletenessDecisionDTO evaluateCompletenessCheckpoint(
            @PathVariable(SEMESTER_ID) @Positive Long semesterId,
            @RequestParam("checkpointDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkpointDate) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        SemesterController.class,
                        "SemesterController.evaluateCompletenessCheckpoint");
        return semesterService.evaluateCompletenessCheckpoint(semesterId, checkpointDate);
    }

    @GetMapping("/{semesterId}/notifications")
    @ApiMessage("Xem danh sách thông báo nhắc điểm của học kỳ")
    @PreAuthorize(OFFICE_ROLES)
    public List<ResSemesterNotificationDTO> listNotifications(
            @PathVariable(SEMESTER_ID) @Positive Long semesterId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        SemesterController.class,
                        "SemesterController.listNotifications");
        return completenessService.getNotificationsForSemester(semesterId);
    }

    @PostMapping("/{semesterId}/notifications/dispatch")
    @ApiMessage("Kích hoạt gửi thông báo nhắc điểm học kỳ")
    @PreAuthorize(OFFICE_ROLES)
    public List<ResSemesterNotificationDTO> dispatchNotifications(
            @PathVariable(SEMESTER_ID) @Positive Long semesterId,
            @RequestParam(name = "checkpointCode", required = false) String checkpointCode) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        SemesterController.class,
                        "SemesterController.dispatchNotifications");
        return completenessService.dispatchCheckpointNotifications(semesterId, checkpointCode);
    }

    @PostMapping("/{semesterId}/notifications/retry-failed")
    @ApiMessage("Thử gửi lại các thông báo nhắc điểm bị lỗi")
    @PreAuthorize(OFFICE_ROLES)
    public List<ResSemesterNotificationDTO> retryFailedNotifications(
            @PathVariable(SEMESTER_ID) @Positive Long semesterId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        SemesterController.class,
                        "SemesterController.retryFailedNotifications");
        return completenessService.retryFailedNotifications(semesterId);
    }
}
