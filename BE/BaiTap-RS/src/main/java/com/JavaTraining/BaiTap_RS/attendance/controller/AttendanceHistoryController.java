package com.JavaTraining.BaiTap_RS.attendance.controller;

import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests.ReqAttendanceHistoryQuery;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResStudentAttendanceHistoryDTO;
import com.JavaTraining.BaiTap_RS.attendance.service.AttendanceHistoryService;
import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2/attendance/students")
@SuppressWarnings("PMD.GuardLogStatement")
public class AttendanceHistoryController {

    private final AttendanceHistoryService attendanceHistoryService;

    public AttendanceHistoryController(AttendanceHistoryService attendanceHistoryService) {
        this.attendanceHistoryService = attendanceHistoryService;
    }

    @GetMapping("/me/history")
    @ApiMessage("Lấy lịch sử chuyên cần của học sinh")
    @PreAuthorize("hasRole('STUDENT')")
    public ResStudentAttendanceHistoryDTO getHistory(@Valid @ModelAttribute ReqAttendanceHistoryQuery query) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                AttendanceHistoryController.class,
                "AttendanceHistoryController.getHistory");
        return attendanceHistoryService.getHistory(query);
    }

    @GetMapping("/{studentId}/history")
    @ApiMessage("Lấy lịch sử chuyên cần của học sinh theo mã định danh")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')")
    public ResStudentAttendanceHistoryDTO getStudentHistory(
            @PathVariable("studentId") @Positive(message = "ID học sinh phải là số dương") Long studentId,
            @Valid @ModelAttribute ReqAttendanceHistoryQuery query) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                AttendanceHistoryController.class,
                "AttendanceHistoryController.getStudentHistory");
        return attendanceHistoryService.getStudentHistory(studentId, query);
    }
}
