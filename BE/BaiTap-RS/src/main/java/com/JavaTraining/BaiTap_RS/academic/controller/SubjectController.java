package com.JavaTraining.BaiTap_RS.academic.controller;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateSubjectApplicabilityDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateSubjectDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateSubjectApplicabilityDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateSubjectDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSubjectApplicabilityDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSubjectDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicabilityStatus;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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

    private static final String ACADEMIC_OFFICE_ROLE = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')";
    private static final String SUBJECT_ID_PATH_VARIABLE = "subjectId";

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
    @PreAuthorize(ACADEMIC_OFFICE_ROLE)
    public ResponseEntity<ResSubjectDTO> createSubject(@Valid @RequestBody ReqCreateSubjectDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subjectService.createSubject(request));
    }

    @PutMapping("/{subjectId}")
    @ApiMessage("Cập nhật môn học")
    @PreAuthorize(ACADEMIC_OFFICE_ROLE)
    public ResSubjectDTO updateSubject(
            @PathVariable(SUBJECT_ID_PATH_VARIABLE) @Positive Long subjectId,
            @Valid @RequestBody ReqUpdateSubjectDTO request) {
        return subjectService.updateSubject(subjectId, request);
    }

    @PostMapping("/{subjectId}/applicabilities")
    @ApiMessage("Cấu hình phạm vi áp dụng môn")
    @PreAuthorize(ACADEMIC_OFFICE_ROLE)
    public ResponseEntity<ResSubjectApplicabilityDTO> createApplicability(
            @PathVariable(SUBJECT_ID_PATH_VARIABLE) @Positive Long subjectId,
            @Valid @RequestBody ReqCreateSubjectApplicabilityDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subjectApplicabilityService.createApplicability(subjectId, request));
    }

    @GetMapping("/{subjectId}/applicabilities")
    @ApiMessage("Lấy danh sách phạm vi áp dụng môn")
    @PreAuthorize(ACADEMIC_OFFICE_ROLE)
    public List<ResSubjectApplicabilityDTO> listApplicabilities(
            @PathVariable(SUBJECT_ID_PATH_VARIABLE) @Positive Long subjectId,
            @RequestParam(value = "semesterId", required = false) @Positive Long semesterId,
            @RequestParam(value = "status", required = false) SubjectApplicabilityStatus status) {
        return subjectApplicabilityService.listApplicabilities(subjectId, semesterId, status);
    }

    @PutMapping("/{subjectId}/applicabilities/{applicabilityId}")
    @ApiMessage("Cập nhật phạm vi áp dụng môn")
    @PreAuthorize(ACADEMIC_OFFICE_ROLE)
    public ResSubjectApplicabilityDTO updateApplicability(
            @PathVariable(SUBJECT_ID_PATH_VARIABLE) @Positive Long subjectId,
            @PathVariable("applicabilityId") @Positive Long applicabilityId,
            @Valid @RequestBody ReqUpdateSubjectApplicabilityDTO request) {
        return subjectApplicabilityService.updateApplicability(subjectId, applicabilityId, request);
    }

    @DeleteMapping("/{subjectId}/applicabilities/{applicabilityId}")
    @ApiMessage("Ngừng áp dụng môn")
    @PreAuthorize(ACADEMIC_OFFICE_ROLE)
    public ResponseEntity<Void> deactivateApplicability(
            @PathVariable(SUBJECT_ID_PATH_VARIABLE) @Positive Long subjectId,
            @PathVariable("applicabilityId") @Positive Long applicabilityId) {
        subjectApplicabilityService.deactivateApplicability(subjectId, applicabilityId);
        return ResponseEntity.noContent().build();
    }
}
