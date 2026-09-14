package com.JavaTraining.BaiTap_RS.lessonlog.controller;

import java.time.LocalDate;
import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.*;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.*;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogRevision;
import com.JavaTraining.BaiTap_RS.lessonlog.service.LessonLogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @Validated @RequiredArgsConstructor @RequestMapping("/api/v3/lesson-logs")
public class LessonLogController {
    private final LessonLogService service;
    @GetMapping("/my-schedule") @PreAuthorize("hasRole('TEACHER')") @ApiMessage("Lấy lịch dạy cá nhân")
    public LessonLogScheduleResponse schedule(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date){return service.mySchedule(date);}
    @GetMapping("/policy") @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC_OFFICE')") @ApiMessage("Lấy policy sổ đầu bài")
    public LessonLogPolicyResponse policy(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date){return service.getPolicy(date);}
    @GetMapping("/classes") @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC_OFFICE','TEACHER')") @ApiMessage("Lấy lớp có sổ đầu bài")
    public List<LessonLogClassResponse> classes(@RequestParam Long semesterId){return service.classes(semesterId);}
    @GetMapping("/class/{classId}") @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC_OFFICE','TEACHER')") @ApiMessage("Lấy sổ đầu bài theo tuần")
    public LessonLogClassWeekResponse classWeek(@PathVariable @Positive Long classId,@RequestParam Long semesterId,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate weekStart){return service.classWeek(classId,semesterId,weekStart);}
    @GetMapping("/class/{classId}/weekly-review/revisions") @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC_OFFICE','TEACHER')") @ApiMessage("Lấy lịch sử ký tuần")
    public ResultPaginationDTO<LessonLogRevisionResponse> weeklyRevisions(@PathVariable @Positive Long classId,@RequestParam Long semesterId,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate weekStart,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int pageSize){return service.weeklyRevisions(classId,semesterId,weekStart,page,pageSize);}
    @GetMapping("/policy/revisions") @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC_OFFICE')") @ApiMessage("Lấy lịch sử policy")
    public ResultPaginationDTO<LessonLogRevisionResponse> policyRevisions(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int pageSize){return service.policyRevisions(page,pageSize);}
    @PutMapping("/policy") @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC_OFFICE')") @ApiMessage("Cập nhật policy sổ đầu bài")
    public LessonLogPolicyResponse putPolicy(@Valid @RequestBody ReqPolicyDTO request){return service.putPolicy(request);}
    @PostMapping("/entries") @PreAuthorize("hasRole('TEACHER')") @ApiMessage("Tạo nháp sổ đầu bài")
    public ResponseEntity<LessonLogEntryResponse> create(@Valid @RequestBody ReqCreateLessonLogDTO request){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));}
    @GetMapping("/entries/{entryId}") @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC_OFFICE','TEACHER')") @ApiMessage("Lấy sổ đầu bài")
    public LessonLogEntryResponse get(@PathVariable @Positive Long entryId){return service.get(entryId);}
    @GetMapping("/entries/{entryId}/revisions") @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC_OFFICE','TEACHER')") @ApiMessage("Lấy lịch sử sổ đầu bài")
    public ResultPaginationDTO<LessonLogRevision> revisions(@PathVariable @Positive Long entryId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int pageSize){return service.revisions(entryId,page,pageSize);}
    @PutMapping("/entries/{entryId}") @PreAuthorize("hasRole('TEACHER')") @ApiMessage("Cập nhật sổ đầu bài")
    public LessonLogEntryResponse update(@PathVariable @Positive Long entryId,@Valid @RequestBody ReqUpdateLessonLogDTO request){return service.update(entryId,request);}
    @PostMapping("/entries/{entryId}/submit") @PreAuthorize("hasRole('TEACHER')") @ApiMessage("Nộp sổ đầu bài")
    public LessonLogEntryResponse submit(@PathVariable @Positive Long entryId,@Valid @RequestBody ReqSubmitLessonLogDTO request){return service.submit(entryId,request.expectedVersion());}
    @PostMapping("/entries/{entryId}/review") @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC_OFFICE')") @ApiMessage("Duyệt sổ đầu bài")
    public LessonLogEntryResponse review(@PathVariable @Positive Long entryId,@Valid @RequestBody ReqTransitionLessonLogDTO request){return service.review(entryId,request);}
    @PostMapping("/entries/{entryId}/amend") @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC_OFFICE')") @ApiMessage("Điều chỉnh sổ đầu bài")
    public LessonLogEntryResponse amend(@PathVariable @Positive Long entryId,@Valid @RequestBody ReqTransitionLessonLogDTO request){return service.amend(entryId,request);}
    @PostMapping("/entries/late-record") @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC_OFFICE')") @ApiMessage("Ghi bổ sung sổ đầu bài")
    public ResponseEntity<LessonLogEntryResponse> late(@Valid @RequestBody ReqLateRecordLessonLogDTO request){return ResponseEntity.status(HttpStatus.CREATED).body(service.lateRecord(request));}
    @PostMapping("/class/{classId}/weekly-review") @PreAuthorize("hasAnyRole('ADMIN','ACADEMIC_OFFICE','TEACHER')") @ApiMessage("Ký sổ đầu bài theo tuần")
    public WeeklyReviewResponse sign(@PathVariable @Positive Long classId,@Valid @RequestBody ReqWeeklyReviewDTO request){return service.signWeek(classId,request);}
}
