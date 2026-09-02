package com.JavaTraining.BaiTap_RS.academic.controller;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateAcademicYearDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateAcademicYearDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResAcademicYearDTO;
import com.JavaTraining.BaiTap_RS.academic.service.AcademicYearService;
import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
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
// FR-AY-001..005 and BR-AUTH-005: academic year mutations are office-only.
@SuppressWarnings("PMD.GuardLogStatement")
public class AcademicYearController {

    private static final String OFFICE_ROLES = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')";

    private final AcademicYearService academicYearService;

    public AcademicYearController(AcademicYearService academicYearService) {
        this.academicYearService = academicYearService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')")
    @ApiMessage("Lấy danh sách năm học")
    public List<ResAcademicYearDTO> listAcademicYears() {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                AcademicYearController.class,
                "AcademicYearController.listAcademicYears");
        return academicYearService.listAcademicYears();
    }

    @PostMapping
    @PreAuthorize(OFFICE_ROLES)
    @ApiMessage("Tạo năm học")
    public ResponseEntity<ResAcademicYearDTO> createAcademicYear(
            @Valid @RequestBody ReqCreateAcademicYearDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                AcademicYearController.class,
                "AcademicYearController.createAcademicYear");
        return ResponseEntity.status(HttpStatus.CREATED).body(academicYearService.createAcademicYear(request));
    }

    @PutMapping("/{academicYearId}")
    @PreAuthorize(OFFICE_ROLES)
    @ApiMessage("Cập nhật năm học")
    public ResAcademicYearDTO updateAcademicYear(
            @PathVariable("academicYearId") @Positive Long academicYearId,
            @Valid @RequestBody ReqUpdateAcademicYearDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                AcademicYearController.class,
                "AcademicYearController.updateAcademicYear");
        return academicYearService.updateAcademicYear(academicYearId, request);
    }

    @PostMapping("/{academicYearId}/close")
    @PreAuthorize(OFFICE_ROLES)
    @ApiMessage("Đóng năm học")
    public ResAcademicYearDTO closeAcademicYear(
            @PathVariable("academicYearId") @Positive Long academicYearId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                AcademicYearController.class,
                "AcademicYearController.closeAcademicYear");
        return academicYearService.closeAcademicYear(academicYearId);
    }

    @DeleteMapping("/{academicYearId}")
    @PreAuthorize(OFFICE_ROLES)
    @ApiMessage("Xóa năm học")
    public ResponseEntity<Void> deleteAcademicYear(
            @PathVariable("academicYearId") @Positive Long academicYearId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                AcademicYearController.class,
                "AcademicYearController.deleteAcademicYear");
        academicYearService.deleteAcademicYear(academicYearId);
        return ResponseEntity.noContent().build();
    }
}
