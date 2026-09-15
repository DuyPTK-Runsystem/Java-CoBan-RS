package com.JavaTraining.BaiTap_RS.lessonlog.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.*;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogEntryResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogRevision;
import com.JavaTraining.BaiTap_RS.lessonlog.service.LessonLogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v3/lesson-logs")
public class LessonLogEntryController {
    private static final String TEACHER_ROLE = "hasRole('TEACHER')";
    private static final String ADMIN_ACADEMIC_ROLE = "hasAnyRole('ADMIN','ACADEMIC_OFFICE')";
    private static final String ADMIN_ACADEMIC_TEACHER_ROLE = "hasAnyRole('ADMIN','ACADEMIC_OFFICE','TEACHER')";

    private final LessonLogService service;

    @PostMapping("/entries")
    @PreAuthorize(TEACHER_ROLE)
    @ApiMessage("Tạo nháp sổ đầu bài")
    public ResponseEntity<LessonLogEntryResponse> create(@Valid @RequestBody ReqCreateLessonLogDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/entries/{entryId}")
    @PreAuthorize(ADMIN_ACADEMIC_TEACHER_ROLE)
    @ApiMessage("Lấy sổ đầu bài")
    public LessonLogEntryResponse get(@PathVariable @Positive Long entryId) {
        return service.get(entryId);
    }

    @GetMapping("/entries/{entryId}/revisions")
    @PreAuthorize(ADMIN_ACADEMIC_TEACHER_ROLE)
    @ApiMessage("Lấy lịch sử sổ đầu bài")
    public ResultPaginationDTO<LessonLogRevision> revisions(@PathVariable @Positive Long entryId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return service.revisions(entryId, page, pageSize);
    }

    @PutMapping("/entries/{entryId}")
    @PreAuthorize(TEACHER_ROLE)
    @ApiMessage("Cập nhật sổ đầu bài")
    public LessonLogEntryResponse update(@PathVariable @Positive Long entryId,
            @Valid @RequestBody ReqUpdateLessonLogDTO request) {
        return service.update(entryId, request);
    }

    @PostMapping("/entries/{entryId}/submit")
    @PreAuthorize(TEACHER_ROLE)
    @ApiMessage("Nộp sổ đầu bài")
    public LessonLogEntryResponse submit(@PathVariable @Positive Long entryId,
            @Valid @RequestBody ReqSubmitLessonLogDTO request) {
        return service.submit(entryId, request.expectedVersion());
    }

    @PostMapping("/entries/{entryId}/review")
    @PreAuthorize(ADMIN_ACADEMIC_ROLE)
    @ApiMessage("Duyệt sổ đầu bài")
    public LessonLogEntryResponse review(@PathVariable @Positive Long entryId,
            @Valid @RequestBody ReqTransitionLessonLogDTO request) {
        return service.review(entryId, request);
    }

    @PostMapping("/entries/{entryId}/amend")
    @PreAuthorize(ADMIN_ACADEMIC_ROLE)
    @ApiMessage("Điều chỉnh sổ đầu bài")
    public LessonLogEntryResponse amend(@PathVariable @Positive Long entryId,
            @Valid @RequestBody ReqTransitionLessonLogDTO request) {
        return service.amend(entryId, request);
    }

    @PostMapping("/entries/late-record")
    @PreAuthorize(ADMIN_ACADEMIC_ROLE)
    @ApiMessage("Ghi bổ sung sổ đầu bài")
    public ResponseEntity<LessonLogEntryResponse> late(
            @Valid @RequestBody ReqLateRecordLessonLogDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.lateRecord(request));
    }
}
