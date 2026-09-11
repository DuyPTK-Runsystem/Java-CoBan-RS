package com.JavaTraining.BaiTap_RS.timetable.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqCreatePolicyDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTeacherLoadPolicyDTO;
import com.JavaTraining.BaiTap_RS.timetable.service.TeacherLoadPolicyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v3/teacher-load-policies")
@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
@RequiredArgsConstructor
public class TeacherLoadPolicyController {

    private final TeacherLoadPolicyService service;

    @GetMapping
    @ApiMessage("Lấy danh sách chính sách định mức tiết dạy")
    public ResultPaginationDTO<ResTeacherLoadPolicyDTO> pagePolicies(Pageable pageable) {
        return service.pagePolicies(pageable);
    }

    @PostMapping
    @ApiMessage("Tạo mới chính sách định mức tiết dạy")
    public ResponseEntity<ResTeacherLoadPolicyDTO> create(@Valid @RequestBody ReqCreatePolicyDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PostMapping("/{id}/activate")
    @ApiMessage("Kích hoạt chính sách định mức tiết dạy")
    public ResTeacherLoadPolicyDTO activate(
            @PathVariable("id") @Positive Long id,
            @RequestParam(name = "expectedVersion", required = false) Long expectedVersion) {
        return service.activate(id, expectedVersion);
    }
}
