package com.JavaTraining.BaiTap_RS.scorebook.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqCreateScoreChangeRequestDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqFilterScoreChangeRequestDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqRejectScoreChangeRequestDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScoreChangeRequestDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScoreChangeRequestDetailDTO;
import com.JavaTraining.BaiTap_RS.scorebook.service.ScoreChangeRequestService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2/score-change-requests")
@SuppressWarnings("PMD.GuardLogStatement")
public class ScoreChangeRequestController {

    private static final String SCORE_CHANGE_ROLES = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')";
    private static final String OFFICE_ROLES = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')";
    private static final String CANCEL_ROLES = "hasAnyRole('ADMIN', 'TEACHER')";
    private static final String REQUEST_ID = "requestId";

    private final ScoreChangeRequestService service;

    public ScoreChangeRequestController(ScoreChangeRequestService service) {
        this.service = service;
    }

    @PostMapping
    @ApiMessage("Tạo yêu cầu sửa điểm")
    @PreAuthorize(SCORE_CHANGE_ROLES)
    public ResponseEntity<ResScoreChangeRequestDetailDTO> create(
            @Valid @RequestBody ReqCreateScoreChangeRequestDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        ScoreChangeRequestController.class,
                        "ScoreChangeRequestController.create");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createRequest(request));
    }

    @GetMapping
    @ApiMessage("Tra cứu yêu cầu sửa điểm")
    @PreAuthorize(SCORE_CHANGE_ROLES)
    public Page<ResScoreChangeRequestDTO> find(@Valid @ModelAttribute ReqFilterScoreChangeRequestDTO filter) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScoreChangeRequestController.class,
                "ScoreChangeRequestController.find");
        return service.findRequests(filter);
    }

    @GetMapping("/{requestId}")
    @ApiMessage("Xem chi tiết yêu cầu sửa điểm")
    @PreAuthorize(SCORE_CHANGE_ROLES)
    public ResScoreChangeRequestDetailDTO get(@PathVariable(REQUEST_ID) @Positive Long requestId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScoreChangeRequestController.class,
                "ScoreChangeRequestController.get");
        return service.getRequest(requestId);
    }

    @PostMapping("/{requestId}/approve")
    @ApiMessage("Phê duyệt và áp dụng yêu cầu sửa điểm")
    @PreAuthorize(OFFICE_ROLES)
    public ResScoreChangeRequestDetailDTO approve(@PathVariable(REQUEST_ID) @Positive Long requestId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScoreChangeRequestController.class,
                "ScoreChangeRequestController.approve");
        return service.approveAndApply(requestId);
    }

    @PostMapping("/{requestId}/reject")
    @ApiMessage("Từ chối yêu cầu sửa điểm")
    @PreAuthorize(OFFICE_ROLES)
    public ResScoreChangeRequestDetailDTO reject(
            @PathVariable(REQUEST_ID) @Positive Long requestId,
            @Valid @RequestBody ReqRejectScoreChangeRequestDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        ScoreChangeRequestController.class,
                        "ScoreChangeRequestController.reject");
        return service.rejectRequest(requestId, request);
    }

    @PostMapping("/{requestId}/cancel")
    @ApiMessage("Hủy yêu cầu sửa điểm")
    @PreAuthorize(CANCEL_ROLES)
    public ResScoreChangeRequestDetailDTO cancel(@PathVariable(REQUEST_ID) @Positive Long requestId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScoreChangeRequestController.class,
                "ScoreChangeRequestController.cancel");
        return service.cancelRequest(requestId);
    }
}
