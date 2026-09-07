package com.JavaTraining.BaiTap_RS.scorebook.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqCreateRetakeExamDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqFilterRetakeExamDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqUpdateRetakeScoreDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResRetakeExamDTO;
import com.JavaTraining.BaiTap_RS.scorebook.service.RetakeExamService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2/retake-exams")
@SuppressWarnings("PMD.GuardLogStatement")
public class RetakeExamController {

    private static final String OFFICE_ROLES = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')";
    private static final String RETAKE_ID = "retakeId";

    private final RetakeExamService retakeExamService;

    public RetakeExamController(RetakeExamService retakeExamService) {
        this.retakeExamService = retakeExamService;
    }

    @PostMapping
    @ApiMessage("Tạo kỳ thi lại")
    @PreAuthorize(OFFICE_ROLES)
    public ResponseEntity<ResRetakeExamDTO> create(
            @Valid @RequestBody ReqCreateRetakeExamDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        RetakeExamController.class,
                        "RetakeExamController.create");
        return ResponseEntity.status(HttpStatus.CREATED).body(retakeExamService.createRetakeExam(request));
    }

    @PutMapping("/{retakeId}/score")
    @ApiMessage("Nhập/cập nhật điểm thi lại")
    @PreAuthorize(OFFICE_ROLES)
    public ResRetakeExamDTO updateScore(
            @PathVariable(RETAKE_ID) @Positive Long retakeId,
            @Valid @RequestBody ReqUpdateRetakeScoreDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        RetakeExamController.class,
                        "RetakeExamController.updateScore");
        return retakeExamService.updateRetakeScore(retakeId, request);
    }

    @PostMapping("/{retakeId}/cancel")
    @ApiMessage("Hủy kỳ thi lại")
    @PreAuthorize(OFFICE_ROLES)
    public ResRetakeExamDTO cancel(
            @PathVariable(RETAKE_ID) @Positive Long retakeId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        RetakeExamController.class,
                        "RetakeExamController.cancel");
        return retakeExamService.cancelRetakeExam(retakeId);
    }

    @GetMapping("/{retakeId}")
    @ApiMessage("Xem chi tiết kỳ thi lại")
    @PreAuthorize(OFFICE_ROLES)
    public ResRetakeExamDTO get(
            @PathVariable(RETAKE_ID) @Positive Long retakeId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        RetakeExamController.class,
                        "RetakeExamController.get");
        return retakeExamService.getRetakeExam(retakeId);
    }

    @GetMapping
    @ApiMessage("Tra cứu danh sách kỳ thi lại")
    @PreAuthorize(OFFICE_ROLES)
    public Page<ResRetakeExamDTO> find(
            @Valid @ModelAttribute ReqFilterRetakeExamDTO filter) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        RetakeExamController.class,
                        "RetakeExamController.find");
        return retakeExamService.findRetakeExams(filter);
    }
}
