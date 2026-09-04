package com.JavaTraining.BaiTap_RS.attendance.controller;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests.ReqCreateAttendanceSessionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests.ReqUpsertAttendanceExceptionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResAttendanceExceptionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResAttendanceSessionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResAttendanceStudentDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSessionPeriod;
import com.JavaTraining.BaiTap_RS.attendance.service.AcademicOfficeAttendanceService;
import com.JavaTraining.BaiTap_RS.attendance.service.AttendanceSessionLookupService;
import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping({"/api/v2/office/attendance-sessions", "/api/v2/academic-office/attendance/sessions"})
@SuppressWarnings("PMD.GuardLogStatement")
public class AcademicOfficeAttendanceController {

    private static final String OFFICE_ROLES = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')";
    private static final String SESSION_ID = "sessionId";
    private static final String STUDENT_ID = "studentId";
    private static final String STUDENT_CODE = "studentCode";

    private final AcademicOfficeAttendanceService attendanceService;
    private final AttendanceSessionLookupService sessionLookupService;

    public AcademicOfficeAttendanceController(
            AcademicOfficeAttendanceService attendanceService,
            AttendanceSessionLookupService sessionLookupService) {
        this.attendanceService = attendanceService;
        this.sessionLookupService = sessionLookupService;
    }

    @PostMapping
    @ApiMessage("Giáo vụ tạo hoặc lấy buổi điểm danh")
    @PreAuthorize(OFFICE_ROLES)
    public ResponseEntity<ResAttendanceSessionDTO> createOrGetSession(
            @Valid @RequestBody ReqCreateAttendanceSessionDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        AcademicOfficeAttendanceController.class,
                        "AcademicOfficeAttendanceController.createOrGetSession",
                        "request classId={}, semesterId={}, attendanceDate={}, sessionPeriod={}",
                        request.classId(), request.semesterId(), request.attendanceDate(), request.sessionPeriod());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceService.createOrGetSession(request));
    }

    @GetMapping
    @ApiMessage("Giáo vụ lấy buổi điểm danh theo context")
    @PreAuthorize(OFFICE_ROLES)
    public ResAttendanceSessionDTO getSession(
            @RequestParam("classId") @Positive Long classId,
            @RequestParam("semesterId") @Positive Long semesterId,
            @RequestParam("attendanceDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate attendanceDate,
            @RequestParam("sessionPeriod") AttendanceSessionPeriod sessionPeriod) {
        return sessionLookupService.getForOffice(new ReqCreateAttendanceSessionDTO(
                classId, semesterId, attendanceDate, sessionPeriod));
    }

    @GetMapping("/{sessionId}/students")
    @ApiMessage("Giáo vụ lấy danh sách điểm danh của buổi")
    @PreAuthorize(OFFICE_ROLES)
    public List<ResAttendanceStudentDTO> listSessionStudents(
            @PathVariable(SESSION_ID) @Positive Long sessionId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        AcademicOfficeAttendanceController.class,
                        "AcademicOfficeAttendanceController.listSessionStudents");
        return attendanceService.listSessionStudents(sessionId);
    }

    @PutMapping("/{sessionId}/exceptions/{studentId}")
    @ApiMessage("Giáo vụ tạo hoặc cập nhật ngoại lệ điểm danh")
    @PreAuthorize(OFFICE_ROLES)
    public ResAttendanceExceptionDTO upsertException(
            @PathVariable(SESSION_ID) @Positive Long sessionId,
            @PathVariable(STUDENT_ID) @Positive Long studentId,
            @Valid @RequestBody ReqUpsertAttendanceExceptionDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        AcademicOfficeAttendanceController.class,
                        "AcademicOfficeAttendanceController.upsertException");
        return attendanceService.upsertException(sessionId, studentId, request);
    }

    @PutMapping("/{sessionId}/exceptions/by-code/{studentCode}")
    @ApiMessage("Giáo vụ tạo hoặc cập nhật ngoại lệ điểm danh theo mã học sinh")
    @PreAuthorize(OFFICE_ROLES)
    public ResAttendanceExceptionDTO upsertExceptionByCode(
            @PathVariable(SESSION_ID) @Positive Long sessionId,
            @PathVariable(STUDENT_CODE) String studentCode,
            @Valid @RequestBody ReqUpsertAttendanceExceptionDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        AcademicOfficeAttendanceController.class,
                        "AcademicOfficeAttendanceController.upsertExceptionByCode");
        return attendanceService.upsertExceptionByCode(sessionId, studentCode, request);
    }

    @DeleteMapping("/{sessionId}/exceptions/{studentId}")
    @ApiMessage("Giáo vụ xóa ngoại lệ điểm danh")
    @PreAuthorize(OFFICE_ROLES)
    public ResponseEntity<Void> deleteException(
            @PathVariable(SESSION_ID) @Positive Long sessionId,
            @PathVariable(STUDENT_ID) @Positive Long studentId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        AcademicOfficeAttendanceController.class,
                        "AcademicOfficeAttendanceController.deleteException");
        attendanceService.deleteException(sessionId, studentId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{sessionId}/exceptions/by-code/{studentCode}")
    @ApiMessage("Giáo vụ xóa ngoại lệ điểm danh theo mã học sinh")
    @PreAuthorize(OFFICE_ROLES)
    public ResponseEntity<Void> deleteExceptionByCode(
            @PathVariable(SESSION_ID) @Positive Long sessionId,
            @PathVariable(STUDENT_CODE) String studentCode) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        AcademicOfficeAttendanceController.class,
                        "AcademicOfficeAttendanceController.deleteExceptionByCode");
        attendanceService.deleteExceptionByCode(sessionId, studentCode);
        return ResponseEntity.noContent().build();
    }
}
