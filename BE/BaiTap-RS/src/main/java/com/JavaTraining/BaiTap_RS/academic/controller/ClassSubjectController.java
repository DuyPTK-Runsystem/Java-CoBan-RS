package com.JavaTraining.BaiTap_RS.academic.controller;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateClassSubjectDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateClassSubjectDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResClassSubjectDTO;
import com.JavaTraining.BaiTap_RS.academic.service.ClassSubjectService;
import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
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
@RequestMapping("/api/v2")
@SuppressWarnings("PMD.GuardLogStatement")
public class ClassSubjectController {

    private final ClassSubjectService classSubjectService;

    public ClassSubjectController(ClassSubjectService classSubjectService) {
        this.classSubjectService = classSubjectService;
    }

    @GetMapping("/classes/{classId}/subjects")
    @ApiMessage("Lấy môn của lớp")
    @PreAuthorize("isAuthenticated()")
    public List<ResClassSubjectDTO> listByClassAndSemester(
            @PathVariable("classId") @Positive Long classId,
            @RequestParam("semesterId") @Positive Long semesterId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        ClassSubjectController.class,
                        "ClassSubjectController.listByClassAndSemester");
        return classSubjectService.listByClassAndSemester(classId, semesterId);
    }

    @PostMapping("/class-subjects")
    @ApiMessage("Tạo lớp-môn")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
    public ResponseEntity<ResClassSubjectDTO> createClassSubject(
            @Valid @RequestBody ReqCreateClassSubjectDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        ClassSubjectController.class,
                        "ClassSubjectController.createClassSubject");
        return ResponseEntity.status(HttpStatus.CREATED).body(classSubjectService.createClassSubject(request));
    }

    @PutMapping("/class-subjects/{classSubjectId}")
    @ApiMessage("Cập nhật lớp-môn")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
    public ResClassSubjectDTO updateClassSubject(
            @PathVariable("classSubjectId") @Positive Long classSubjectId,
            @Valid @RequestBody ReqUpdateClassSubjectDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        ClassSubjectController.class,
                        "ClassSubjectController.updateClassSubject");
        return classSubjectService.updateClassSubject(classSubjectId, request);
    }
}
