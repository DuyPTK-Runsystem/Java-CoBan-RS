package com.JavaTraining.BaiTap_RS.timetable.controller;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqCreateRevisionDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqCreateTimetableDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqPublishTimetableDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqUpdateTimetableEntriesDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTimetableDetailDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTimetableEntryDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTimetableReviewDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTimetableSummaryDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.SessionType;
import com.JavaTraining.BaiTap_RS.timetable.service.TimetablePublishService;
import com.JavaTraining.BaiTap_RS.timetable.service.TimetableService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v3/timetables")
@RequiredArgsConstructor
@SuppressWarnings("PMD.ExcessiveImports")
public class TimetableController {

    private static final String ROLE_ADMIN_OFFICE = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')";
    private static final String ROLE_ADMIN_OFFICE_TEACHER = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')";

    private final TimetableService timetableService;
    private final TimetablePublishService publishService;

    @GetMapping
    @PreAuthorize(ROLE_ADMIN_OFFICE)
    @ApiMessage("Lấy danh sách thời khóa biểu")
    public ResultPaginationDTO<ResTimetableSummaryDTO> pageSummaries(
            @RequestParam("semesterId") @Positive Long semesterId,
            Pageable pageable) {
        return timetableService.pageSummaries(semesterId, pageable);
    }

    @PostMapping
    @PreAuthorize(ROLE_ADMIN_OFFICE)
    @ApiMessage("Tạo mới thời khóa biểu")
    public ResponseEntity<ResTimetableDetailDTO> createDraft(
            @Valid @RequestBody ReqCreateTimetableDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timetableService.createDraft(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize(ROLE_ADMIN_OFFICE_TEACHER)
    @ApiMessage("Xem chi tiết thời khóa biểu")
    public ResTimetableDetailDTO getDetail(@PathVariable("id") @Positive Long id) {
        return timetableService.getDetail(id);
    }

    @GetMapping("/{id}/entries")
    @PreAuthorize(ROLE_ADMIN_OFFICE_TEACHER)
    @ApiMessage("Lấy danh sách tiết học theo tuần")
    public List<ResTimetableEntryDTO> getEntries(
            @PathVariable("id") @Positive Long id,
            @RequestParam(name = "weekStart", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam(name = "classId", required = false) Long classId,
            @RequestParam(name = "teacherId", required = false) Long teacherId,
            @RequestParam(name = "functionalRoomId", required = false) Long functionalRoomId,
            @RequestParam(name = "session", required = false) SessionType session,
            @RequestParam(name = "periodId", required = false) Long periodId) {
        return timetableService.getEntries(id, weekStart, classId, teacherId, functionalRoomId, session, periodId);
    }

    @PutMapping("/{id}/entries")
    @PreAuthorize(ROLE_ADMIN_OFFICE)
    @ApiMessage("Cập nhật danh sách tiết học")
    public ResTimetableDetailDTO updateEntries(
            @PathVariable("id") @Positive Long id,
            @Valid @RequestBody ReqUpdateTimetableEntriesDTO req) {
        return timetableService.updateEntries(id, req);
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize(ROLE_ADMIN_OFFICE)
    @ApiMessage("Kiểm tra thời khóa biểu")
    public ResTimetableReviewDTO validate(
            @PathVariable("id") @Positive Long id,
            @RequestParam(name = "expectedVersion", required = false) Long expectedVersion) {
        return timetableService.validate(id, expectedVersion);
    }

    @GetMapping("/{id}/review")
    @PreAuthorize(ROLE_ADMIN_OFFICE)
    @ApiMessage("Xem kết quả kiểm tra thời khóa biểu")
    public ResTimetableReviewDTO getReview(@PathVariable("id") @Positive Long id) {
        return timetableService.getReview(id);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize(ROLE_ADMIN_OFFICE)
    @ApiMessage("Công bố thời khóa biểu")
    public ResTimetableDetailDTO publish(
            @PathVariable("id") @Positive Long id,
            @Valid @RequestBody ReqPublishTimetableDTO req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return publishService.publish(id, req, idempotencyKey);
    }

    @PostMapping("/{id}/revisions")
    @PreAuthorize(ROLE_ADMIN_OFFICE)
    @ApiMessage("Tạo bản điều chỉnh mới")
    public ResponseEntity<ResTimetableDetailDTO> createRevision(
            @PathVariable("id") @Positive Long id,
            @Valid @RequestBody ReqCreateRevisionDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timetableService.createRevision(id, req));
    }
}
