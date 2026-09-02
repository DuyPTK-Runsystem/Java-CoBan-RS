package com.JavaTraining.BaiTap_RS.academic.controller;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSemesterCompletenessDecisionDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSemesterCompletenessReportDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSemesterNotificationDTO;
import com.JavaTraining.BaiTap_RS.academic.service.SemesterCompletenessService;
import com.JavaTraining.BaiTap_RS.academic.service.SemesterService;
import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2/semesters")
@SuppressWarnings("PMD.GuardLogStatement")
public class SemesterCompletenessController {

    private static final String OFFICE_ROLES = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')";
    private static final String SEMESTER_ID = "semesterId";

    private final SemesterService semesterService;
    private final SemesterCompletenessService completenessService;

    public SemesterCompletenessController(
            SemesterService semesterService,
            SemesterCompletenessService completenessService) {
        this.semesterService = semesterService;
        this.completenessService = completenessService;
    }

    @GetMapping("/{semesterId}/completeness-report")
    @ApiMessage("Xem báo cáo mức độ hoàn thành nhập điểm")
    @PreAuthorize(OFFICE_ROLES)
    public ResSemesterCompletenessReportDTO getCompletenessReport(
            @PathVariable(SEMESTER_ID) @Positive Long semesterId,
            @RequestParam(name = "checkpointCode", required = false) String checkpointCode) {
        trace("SemesterCompletenessController.getCompletenessReport");
        return completenessService.getLatestReport(semesterId, checkpointCode);
    }

    @GetMapping("/{semesterId}/completeness-decision")
    @ApiMessage("Kiểm tra checkpoint dữ liệu điểm")
    @PreAuthorize(OFFICE_ROLES)
    public ResSemesterCompletenessDecisionDTO evaluateCompletenessCheckpoint(
            @PathVariable(SEMESTER_ID) @Positive Long semesterId,
            @RequestParam("checkpointDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkpointDate) {
        trace("SemesterCompletenessController.evaluateCompletenessCheckpoint");
        return semesterService.evaluateCompletenessCheckpoint(semesterId, checkpointDate);
    }

    @GetMapping("/{semesterId}/notifications")
    @ApiMessage("Xem danh sách thông báo nhắc điểm của học kỳ")
    @PreAuthorize(OFFICE_ROLES)
    public List<ResSemesterNotificationDTO> listNotifications(
            @PathVariable(SEMESTER_ID) @Positive Long semesterId) {
        trace("SemesterCompletenessController.listNotifications");
        return completenessService.getNotificationsForSemester(semesterId);
    }

    @PostMapping("/{semesterId}/notifications/dispatch")
    @ApiMessage("Kích hoạt gửi thông báo nhắc điểm học kỳ")
    @PreAuthorize(OFFICE_ROLES)
    public List<ResSemesterNotificationDTO> dispatchNotifications(
            @PathVariable(SEMESTER_ID) @Positive Long semesterId,
            @RequestParam(name = "checkpointCode", required = false) String checkpointCode) {
        trace("SemesterCompletenessController.dispatchNotifications");
        return completenessService.dispatchCheckpointNotifications(semesterId, checkpointCode);
    }

    @PostMapping("/{semesterId}/notifications/retry-failed")
    @ApiMessage("Thử gửi lại các thông báo nhắc điểm bị lỗi")
    @PreAuthorize(OFFICE_ROLES)
    public List<ResSemesterNotificationDTO> retryFailedNotifications(
            @PathVariable(SEMESTER_ID) @Positive Long semesterId) {
        trace("SemesterCompletenessController.retryFailedNotifications");
        return completenessService.retryFailedNotifications(semesterId);
    }

    private void trace(String operation) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */ SemesterCompletenessController.class, operation);
    }
}
