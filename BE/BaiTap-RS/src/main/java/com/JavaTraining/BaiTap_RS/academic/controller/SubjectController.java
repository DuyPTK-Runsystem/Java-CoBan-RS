package com.JavaTraining.BaiTap_RS.academic.controller;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateSubjectApplicabilityDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateSubjectDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateSubjectDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSubjectApplicabilityDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSubjectDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.service.SubjectApplicabilityService;
import com.JavaTraining.BaiTap_RS.academic.service.SubjectService;
import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
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
@RequestMapping("/api/v2/subjects")
public class SubjectController {

    private final SubjectService subjectService;
    private final SubjectApplicabilityService subjectApplicabilityService;

    public SubjectController(
            SubjectService subjectService,
            SubjectApplicabilityService subjectApplicabilityService) {
        this.subjectService = subjectService;
        this.subjectApplicabilityService = subjectApplicabilityService;
    }

    @GetMapping
    @ApiMessage("Lấy danh sách môn học")
    @PreAuthorize("isAuthenticated()")
    public List<ResSubjectDTO> listSubjects(@RequestParam(value = "status", required = false) SubjectStatus status) {
        return subjectService.listSubjects(status);
    }

    @PostMapping
    @ApiMessage("Tạo môn học")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
    public ResponseEntity<ResSubjectDTO> createSubject(@Valid @RequestBody ReqCreateSubjectDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subjectService.createSubject(request));
    }

    @PutMapping("/{subjectId}")
    @ApiMessage("Cập nhật môn học")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
    public ResSubjectDTO updateSubject(
            @PathVariable("subjectId") @Positive Long subjectId,
            @Valid @RequestBody ReqUpdateSubjectDTO request) {
        return subjectService.updateSubject(subjectId, request);
    }

    @PostMapping("/{subjectId}/applicabilities")
    @ApiMessage("Cấu hình phạm vi áp dụng môn")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
    public ResponseEntity<ResSubjectApplicabilityDTO> createApplicability(
            @PathVariable("subjectId") @Positive Long subjectId,
            @Valid @RequestBody ReqCreateSubjectApplicabilityDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subjectApplicabilityService.createApplicability(subjectId, request));
    }
}
