package com.JavaTraining.BaiTap_RS.timetable.controller;

import java.util.List;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqCreateEligibilityDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqUpdateEligibilityDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTeacherLoadEligibilityDTO;
import com.JavaTraining.BaiTap_RS.timetable.service.TeacherLoadEligibilityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v3/teacher-load-eligibilities")
@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
@RequiredArgsConstructor
public class TeacherLoadEligibilityController {

    private final TeacherLoadEligibilityService service;

    @GetMapping
    @ApiMessage("Lấy danh sách hồ sơ miễn giảm tiết dạy")
    public List<ResTeacherLoadEligibilityDTO> list(
            @RequestParam(name = "teacherId", required = false) Long teacherId) {
        return service.listByTeacher(teacherId);
    }

    @PostMapping
    @ApiMessage("Tạo mới hồ sơ miễn giảm tiết dạy")
    public ResponseEntity<ResTeacherLoadEligibilityDTO> create(@Valid @RequestBody ReqCreateEligibilityDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật hồ sơ miễn giảm tiết dạy")
    public ResTeacherLoadEligibilityDTO update(
            @PathVariable("id") @Positive Long id,
            @Valid @RequestBody ReqUpdateEligibilityDTO req) {
        return service.update(id, req);
    }
}
