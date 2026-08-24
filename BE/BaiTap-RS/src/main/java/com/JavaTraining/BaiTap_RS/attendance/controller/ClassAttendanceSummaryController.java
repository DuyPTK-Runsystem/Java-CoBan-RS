package com.JavaTraining.BaiTap_RS.attendance.controller;

import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.requests.ReqClassAttendanceSummaryQuery;
import com.JavaTraining.BaiTap_RS.attendance.domain.DTOs.response.ResClassAttendanceSummaryDTO;
import com.JavaTraining.BaiTap_RS.attendance.service.ClassAttendanceSummaryService;
import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
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
@RequestMapping("/api/v2/attendance/classes")
public class ClassAttendanceSummaryController {

    private final ClassAttendanceSummaryService summaryService;

    public ClassAttendanceSummaryController(ClassAttendanceSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/{classId}/summary")
    @ApiMessage("Lấy báo cáo chuyên cần của lớp")
    @PreAuthorize("hasRole('TEACHER')")
    public ResClassAttendanceSummaryDTO getClassSummary(
            @PathVariable("classId") @Positive(message = "ID lớp phải là số dương") Long classId,
            @Valid @ModelAttribute ReqClassAttendanceSummaryQuery query) {
        return summaryService.getClassSummary(classId, query);
    }
}
