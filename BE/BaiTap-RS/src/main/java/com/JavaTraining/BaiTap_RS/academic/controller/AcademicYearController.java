package com.JavaTraining.BaiTap_RS.academic.controller;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateAcademicYearDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateAcademicYearDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResAcademicYearDTO;
import com.JavaTraining.BaiTap_RS.academic.service.AcademicYearService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2/academic-years")
@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
// FR-AY-001..005 and BR-AUTH-005: academic year mutations are office-only.
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    public AcademicYearController(AcademicYearService academicYearService) {
        this.academicYearService = academicYearService;
    }

    @GetMapping
    @ApiMessage("Lấy danh sách năm học")
    public List<ResAcademicYearDTO> listAcademicYears() {
        return academicYearService.listAcademicYears();
    }

    @PostMapping
    @ApiMessage("Tạo năm học")
    public ResponseEntity<ResAcademicYearDTO> createAcademicYear(
            @Valid @RequestBody ReqCreateAcademicYearDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(academicYearService.createAcademicYear(request));
    }

    @PutMapping("/{academicYearId}")
    @ApiMessage("Cập nhật năm học")
    public ResAcademicYearDTO updateAcademicYear(
            @PathVariable("academicYearId") @Positive Long academicYearId,
            @Valid @RequestBody ReqUpdateAcademicYearDTO request) {
        return academicYearService.updateAcademicYear(academicYearId, request);
    }

    @PostMapping("/{academicYearId}/close")
    @ApiMessage("Đóng năm học")
    public ResAcademicYearDTO closeAcademicYear(
            @PathVariable("academicYearId") @Positive Long academicYearId) {
        return academicYearService.closeAcademicYear(academicYearId);
    }

    @DeleteMapping("/{academicYearId}")
    @ApiMessage("Xóa năm học")
    public ResponseEntity<Void> deleteAcademicYear(
            @PathVariable("academicYearId") @Positive Long academicYearId) {
        academicYearService.deleteAcademicYear(academicYearId);
        return ResponseEntity.noContent().build();
    }
}
