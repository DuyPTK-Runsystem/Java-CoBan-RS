package com.JavaTraining.BaiTap_RS.attendance.controller;

import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests.ReqAttendanceHistoryQuery;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResStudentAttendanceHistoryDTO;
import com.JavaTraining.BaiTap_RS.attendance.service.AttendanceHistoryService;
import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2/attendance/students/me")
@SuppressWarnings("PMD.GuardLogStatement")
public class AttendanceHistoryController {

    private final AttendanceHistoryService attendanceHistoryService;

    public AttendanceHistoryController(AttendanceHistoryService attendanceHistoryService) {
        this.attendanceHistoryService = attendanceHistoryService;
    }

    @GetMapping("/history")
    @ApiMessage("Lấy lịch sử chuyên cần của học sinh")
    @PreAuthorize("hasRole('STUDENT')")
    public ResStudentAttendanceHistoryDTO getHistory(@Valid @ModelAttribute ReqAttendanceHistoryQuery query) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                AttendanceHistoryController.class,
                "AttendanceHistoryController.getHistory");
        return attendanceHistoryService.getHistory(query);
    }
}
