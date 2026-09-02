package com.JavaTraining.BaiTap_RS.academic.controller;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateGradeLevelDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateGradeLevelDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResGradeLevelDTO;
import com.JavaTraining.BaiTap_RS.academic.service.GradeLevelService;
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
@RequestMapping("/api/v2/grades")
@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
// FR-GRADE-001..005 and BR-AUTH-005: grade metadata mutations are office-only.
@SuppressWarnings("PMD.GuardLogStatement")
public class GradeLevelController {

    private final GradeLevelService gradeLevelService;

    public GradeLevelController(GradeLevelService gradeLevelService) {
        this.gradeLevelService = gradeLevelService;
    }

    @GetMapping
    @ApiMessage("Lấy danh sách khối")
    public List<ResGradeLevelDTO> listGradeLevels() {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                GradeLevelController.class,
                "GradeLevelController.listGradeLevels");
        return gradeLevelService.listGradeLevels();
    }

    @PostMapping
    @ApiMessage("Tạo khối")
    public ResponseEntity<ResGradeLevelDTO> createGradeLevel(
            @Valid @RequestBody ReqCreateGradeLevelDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        GradeLevelController.class,
                        "GradeLevelController.createGradeLevel");
        return ResponseEntity.status(HttpStatus.CREATED).body(gradeLevelService.createGradeLevel(request));
    }

    @PutMapping("/{gradeId}")
    @ApiMessage("Cập nhật khối")
    public ResGradeLevelDTO updateGradeLevel(
            @PathVariable("gradeId") @Positive Long gradeId,
            @Valid @RequestBody ReqUpdateGradeLevelDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        GradeLevelController.class,
                        "GradeLevelController.updateGradeLevel");
        return gradeLevelService.updateGradeLevel(gradeId, request);
    }

    @DeleteMapping("/{gradeId}")
    @ApiMessage("Xóa khối")
    public ResponseEntity<Void> deleteGradeLevel(@PathVariable("gradeId") @Positive Long gradeId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                GradeLevelController.class,
                "GradeLevelController.deleteGradeLevel");
        gradeLevelService.deleteGradeLevel(gradeId);
        return ResponseEntity.noContent().build();
    }
}
