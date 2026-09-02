package com.JavaTraining.BaiTap_RS.attendance.controller;

import java.util.List;

import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests.ReqCreateAttendanceSessionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests.ReqUpsertAttendanceExceptionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResAttendanceExceptionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResAttendanceSessionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResAttendanceStudentDTO;
import com.JavaTraining.BaiTap_RS.attendance.service.AttendanceService;
import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping({"/api/v2/attendance-sessions", "/api/v2/attendance/sessions"})
@SuppressWarnings("PMD.GuardLogStatement")
public class AttendanceController {

    private static final String TEACHER_ROLE = "hasRole('TEACHER')";
    private static final String SESSION_ID = "sessionId";
    private static final String STUDENT_ID = "studentId";
    private static final String STUDENT_CODE = "studentCode";

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping
    @ApiMessage("Tạo hoặc lấy buổi điểm danh")
    @PreAuthorize(TEACHER_ROLE)
    public ResponseEntity<ResAttendanceSessionDTO> createOrGetSession(
            @Valid @RequestBody ReqCreateAttendanceSessionDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        AttendanceController.class,
                        "AttendanceController.createOrGetSession");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceService.createOrGetSession(request));
    }

    @GetMapping("/{sessionId}/students")
    @ApiMessage("Lấy danh sách điểm danh của buổi")
    @PreAuthorize(TEACHER_ROLE)
    public List<ResAttendanceStudentDTO> listSessionStudents(
            @PathVariable(SESSION_ID) @Positive Long sessionId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        AttendanceController.class,
                        "AttendanceController.listSessionStudents");
        return attendanceService.listSessionStudents(sessionId);
    }

    @PutMapping("/{sessionId}/exceptions/{studentId}")
    @ApiMessage("Tạo hoặc cập nhật ngoại lệ điểm danh")
    @PreAuthorize(TEACHER_ROLE)
    public ResAttendanceExceptionDTO upsertException(
            @PathVariable(SESSION_ID) @Positive Long sessionId,
            @PathVariable(STUDENT_ID) @Positive Long studentId,
            @Valid @RequestBody ReqUpsertAttendanceExceptionDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        AttendanceController.class,
                        "AttendanceController.upsertException");
        return attendanceService.upsertException(sessionId, studentId, request);
    }

    @PutMapping("/{sessionId}/exceptions/by-code/{studentCode}")
    @ApiMessage("Tạo hoặc cập nhật ngoại lệ điểm danh theo mã học sinh")
    @PreAuthorize(TEACHER_ROLE)
    public ResAttendanceExceptionDTO upsertExceptionByCode(
            @PathVariable(SESSION_ID) @Positive Long sessionId,
            @PathVariable(STUDENT_CODE) String studentCode,
            @Valid @RequestBody ReqUpsertAttendanceExceptionDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        AttendanceController.class,
                        "AttendanceController.upsertExceptionByCode");
        return attendanceService.upsertExceptionByCode(sessionId, studentCode, request);
    }

    @DeleteMapping("/{sessionId}/exceptions/{studentId}")
    @ApiMessage("Xóa ngoại lệ điểm danh")
    @PreAuthorize(TEACHER_ROLE)
    public ResponseEntity<Void> deleteException(
            @PathVariable(SESSION_ID) @Positive Long sessionId,
            @PathVariable(STUDENT_ID) @Positive Long studentId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        AttendanceController.class,
                        "AttendanceController.deleteException");
        attendanceService.deleteException(sessionId, studentId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{sessionId}/exceptions/by-code/{studentCode}")
    @ApiMessage("Xóa ngoại lệ điểm danh theo mã học sinh")
    @PreAuthorize(TEACHER_ROLE)
    public ResponseEntity<Void> deleteExceptionByCode(
            @PathVariable(SESSION_ID) @Positive Long sessionId,
            @PathVariable(STUDENT_CODE) String studentCode) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        AttendanceController.class,
                        "AttendanceController.deleteExceptionByCode");
        attendanceService.deleteExceptionByCode(sessionId, studentCode);
        return ResponseEntity.noContent().build();
    }
}
