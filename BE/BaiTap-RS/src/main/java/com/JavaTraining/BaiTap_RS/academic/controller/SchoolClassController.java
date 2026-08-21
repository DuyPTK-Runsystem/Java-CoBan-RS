package com.JavaTraining.BaiTap_RS.academic.controller;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateSchoolClassDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateSchoolClassDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSchoolClassDTO;
import com.JavaTraining.BaiTap_RS.academic.service.SchoolClassService;
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
@RequestMapping("/api/v2/classes")
@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
// FR-CLASS-001..011 and BR-AUTH-005: class metadata mutations are office-only.
public class SchoolClassController {

    private final SchoolClassService schoolClassService;

    public SchoolClassController(SchoolClassService schoolClassService) {
        this.schoolClassService = schoolClassService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')")
    @ApiMessage("Lấy danh sách lớp")
    public List<ResSchoolClassDTO> listSchoolClasses(
            @RequestParam(value = "academicYearId", required = false) @Positive Long academicYearId) {
        return schoolClassService.listSchoolClasses(academicYearId);
    }

    @PostMapping
    @ApiMessage("Tạo lớp")
    public ResponseEntity<ResSchoolClassDTO> createSchoolClass(
            @Valid @RequestBody ReqCreateSchoolClassDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schoolClassService.createSchoolClass(request));
    }

    @PutMapping("/{classId}")
    @ApiMessage("Cập nhật lớp")
    public ResSchoolClassDTO updateSchoolClass(
            @PathVariable("classId") @Positive Long classId,
            @Valid @RequestBody ReqUpdateSchoolClassDTO request) {
        return schoolClassService.updateSchoolClass(classId, request);
    }

    @PostMapping("/{classId}/close")
    @ApiMessage("Đóng lớp")
    public ResSchoolClassDTO closeSchoolClass(@PathVariable("classId") @Positive Long classId) {
        return schoolClassService.closeSchoolClass(classId);
    }

    @DeleteMapping("/{classId}")
    @ApiMessage("Xóa lớp")
    public ResponseEntity<Void> deleteSchoolClass(@PathVariable("classId") @Positive Long classId) {
        schoolClassService.deleteSchoolClass(classId);
        return ResponseEntity.noContent().build();
    }
}
