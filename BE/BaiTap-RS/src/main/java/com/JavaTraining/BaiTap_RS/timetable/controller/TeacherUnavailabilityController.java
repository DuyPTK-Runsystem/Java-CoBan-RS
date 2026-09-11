package com.JavaTraining.BaiTap_RS.timetable.controller;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqCreateUnavailabilityDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqRejectUnavailabilityDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqUpdateUnavailabilityDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTeacherUnavailabilityDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherUnavailabilityStatus;
import com.JavaTraining.BaiTap_RS.timetable.service.TeacherUnavailabilityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/api/v3/teacher-unavailability")
@RequiredArgsConstructor
@SuppressWarnings("PMD.ExcessiveImports")
public class TeacherUnavailabilityController {

    private static final String ROLE_ADMIN_OFFICE_TEACHER = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')";

    private final TeacherUnavailabilityService service;
    private final TeacherRepository teacherRepository;

    @GetMapping
    @PreAuthorize(ROLE_ADMIN_OFFICE_TEACHER)
    @ApiMessage("Lấy danh sách đăng ký lịch bận")
    public List<ResTeacherUnavailabilityDTO> list(
            @RequestParam(name = "semesterId", required = false) Long semesterId,
            @RequestParam(name = "teacherId", required = false) Long teacherId,
            @RequestParam(name = "status", required = false) TeacherUnavailabilityStatus status,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long currentTeacherId = resolveCurrentTeacherId();
        boolean isTeacher = isTeacherOnly();
        Long effectiveTeacherId = isTeacher ? currentTeacherId : teacherId;
        return service.list(semesterId, effectiveTeacherId, status, from, to);
    }

    @PostMapping
    @PreAuthorize(ROLE_ADMIN_OFFICE_TEACHER)
    @ApiMessage("Đăng ký lịch bận")
    public ResponseEntity<ResTeacherUnavailabilityDTO> create(@Valid @RequestBody ReqCreateUnavailabilityDTO req) {
        Long currentTeacherId = resolveCurrentTeacherId();
        boolean isTeacher = isTeacherOnly();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, currentTeacherId, isTeacher));
    }

    @PutMapping("/{id}")
    @PreAuthorize(ROLE_ADMIN_OFFICE_TEACHER)
    @ApiMessage("Cập nhật đơn lịch bận")
    public ResTeacherUnavailabilityDTO update(
            @PathVariable("id") @Positive Long id,
            @Valid @RequestBody ReqUpdateUnavailabilityDTO req) {
        Long currentTeacherId = resolveCurrentTeacherId();
        boolean isTeacher = isTeacherOnly();
        return service.update(id, req, currentTeacherId, isTeacher);
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize(ROLE_ADMIN_OFFICE_TEACHER)
    @ApiMessage("Rút đơn đăng ký lịch bận")
    public ResTeacherUnavailabilityDTO withdraw(
            @PathVariable("id") @Positive Long id,
            @RequestParam(name = "expectedVersion", required = false) Long expectedVersion) {
        Long currentTeacherId = resolveCurrentTeacherId();
        boolean isTeacher = isTeacherOnly();
        return service.withdraw(id, expectedVersion, currentTeacherId, isTeacher);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
    @ApiMessage("Duyệt đơn đăng ký lịch bận")
    public ResTeacherUnavailabilityDTO approve(
            @PathVariable("id") @Positive Long id,
            @RequestParam(name = "expectedVersion", required = false) Long expectedVersion) {
        return service.approve(id, expectedVersion, AuditContext.currentUserId());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
    @ApiMessage("Từ chối đơn đăng ký lịch bận")
    public ResTeacherUnavailabilityDTO reject(
            @PathVariable("id") @Positive Long id,
            @Valid @RequestBody ReqRejectUnavailabilityDTO req) {
        return service.reject(id, req, AuditContext.currentUserId());
    }

    private Long resolveCurrentTeacherId() {
        Long userId = AuditContext.currentUserId();
        if (userId == null) {
            return null;
        }
        return teacherRepository.findByUserId(userId).map(Teacher::getId).orElse(null);
    }

    private boolean isTeacherOnly() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        boolean hasAdminOrOffice = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())
                        || "ROLE_ACADEMIC_OFFICE".equals(a.getAuthority()));
        return !hasAdminOrOffice && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_TEACHER".equals(a.getAuthority()));
    }
}
