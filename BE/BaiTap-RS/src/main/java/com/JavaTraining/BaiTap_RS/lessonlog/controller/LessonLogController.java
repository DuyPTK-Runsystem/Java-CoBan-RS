package com.JavaTraining.BaiTap_RS.lessonlog.controller;

import java.time.LocalDate;
import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.*;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.*;
import com.JavaTraining.BaiTap_RS.lessonlog.service.LessonLogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v3/lesson-logs")
public class LessonLogController {
    private static final String TEACHER_ROLE = "hasRole('TEACHER')";
    private static final String ADMIN_ACADEMIC_ROLE = "hasAnyRole('ADMIN','ACADEMIC_OFFICE')";
    private static final String ADMIN_ACADEMIC_TEACHER_ROLE = "hasAnyRole('ADMIN','ACADEMIC_OFFICE','TEACHER')";

    private final LessonLogService service;

    @GetMapping("/my-schedule")
    @PreAuthorize(TEACHER_ROLE)
    @ApiMessage("Lấy lịch dạy cá nhân")
    public LessonLogScheduleResponse schedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.mySchedule(date);
    }

    @GetMapping("/policy")
    @PreAuthorize(ADMIN_ACADEMIC_ROLE)
    @ApiMessage("Lấy policy sổ đầu bài")
    public LessonLogPolicyResponse policy(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.getPolicy(date);
    }

    @GetMapping("/classes")
    @PreAuthorize(ADMIN_ACADEMIC_TEACHER_ROLE)
    @ApiMessage("Lấy lớp có sổ đầu bài")
    public List<LessonLogClassResponse> classes(@RequestParam Long semesterId) {
        return service.classes(semesterId);
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize(ADMIN_ACADEMIC_TEACHER_ROLE)
    @ApiMessage("Lấy sổ đầu bài theo tuần")
    public LessonLogClassWeekResponse classWeek(@PathVariable @Positive Long classId, @RequestParam Long semesterId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return service.classWeek(classId, semesterId, weekStart);
    }

    @GetMapping("/class/{classId}/weekly-review/revisions")
    @PreAuthorize(ADMIN_ACADEMIC_TEACHER_ROLE)
    @ApiMessage("Lấy lịch sử ký tuần")
    public ResultPaginationDTO<LessonLogRevisionResponse> weeklyRevisions(@PathVariable @Positive Long classId,
            @RequestParam Long semesterId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return service.weeklyRevisions(classId, semesterId, weekStart, page, pageSize);
    }

    @GetMapping("/policy/revisions")
    @PreAuthorize(ADMIN_ACADEMIC_ROLE)
    @ApiMessage("Lấy lịch sử policy")
    public ResultPaginationDTO<LessonLogRevisionResponse> policyRevisions(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return service.policyRevisions(page, pageSize);
    }

    @PutMapping("/policy")
    @PreAuthorize(ADMIN_ACADEMIC_ROLE)
    @ApiMessage("Cập nhật policy sổ đầu bài")
    public LessonLogPolicyResponse putPolicy(@Valid @RequestBody ReqPolicyDTO request) {
        return service.putPolicy(request);
    }

    @PostMapping("/class/{classId}/weekly-review")
    @PreAuthorize(ADMIN_ACADEMIC_TEACHER_ROLE)
    @ApiMessage("Ký sổ đầu bài theo tuần")
    public WeeklyReviewResponse sign(@PathVariable @Positive Long classId,
            @Valid @RequestBody ReqWeeklyReviewDTO request) {
        return service.signWeek(classId, request);
    }
}
