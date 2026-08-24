package com.JavaTraining.BaiTap_RS.attendance.controller;

import java.util.List;

import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests.ReqCreateAttendanceSessionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests.ReqUpsertAttendanceExceptionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResAttendanceExceptionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResAttendanceSessionDTO;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResAttendanceStudentDTO;
import com.JavaTraining.BaiTap_RS.attendance.service.AcademicOfficeAttendanceService;
import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
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
@RequestMapping("/api/v2/office/attendance-sessions")
public class AcademicOfficeAttendanceController {

    private static final String OFFICE_ROLES = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')";
    private static final String SESSION_ID = "sessionId";
    private static final String STUDENT_ID = "studentId";

    private final AcademicOfficeAttendanceService attendanceService;

    public AcademicOfficeAttendanceController(AcademicOfficeAttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping
    @ApiMessage("Giáo vụ tạo hoặc lấy buổi điểm danh")
    @PreAuthorize(OFFICE_ROLES)
    public ResponseEntity<ResAttendanceSessionDTO> createOrGetSession(
            @Valid @RequestBody ReqCreateAttendanceSessionDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceService.createOrGetSession(request));
    }

    @GetMapping("/{sessionId}/students")
    @ApiMessage("Giáo vụ lấy danh sách điểm danh của buổi")
    @PreAuthorize(OFFICE_ROLES)
    public List<ResAttendanceStudentDTO> listSessionStudents(
            @PathVariable(SESSION_ID) @Positive Long sessionId) {
        return attendanceService.listSessionStudents(sessionId);
    }

    @PutMapping("/{sessionId}/exceptions/{studentId}")
    @ApiMessage("Giáo vụ tạo hoặc cập nhật ngoại lệ điểm danh")
    @PreAuthorize(OFFICE_ROLES)
    public ResAttendanceExceptionDTO upsertException(
            @PathVariable(SESSION_ID) @Positive Long sessionId,
            @PathVariable(STUDENT_ID) @Positive Long studentId,
            @Valid @RequestBody ReqUpsertAttendanceExceptionDTO request) {
        return attendanceService.upsertException(sessionId, studentId, request);
    }

    @DeleteMapping("/{sessionId}/exceptions/{studentId}")
    @ApiMessage("Giáo vụ xóa ngoại lệ điểm danh")
    @PreAuthorize(OFFICE_ROLES)
    public ResponseEntity<Void> deleteException(
            @PathVariable(SESSION_ID) @Positive Long sessionId,
            @PathVariable(STUDENT_ID) @Positive Long studentId) {
        attendanceService.deleteException(sessionId, studentId);
        return ResponseEntity.noContent().build();
    }
}
