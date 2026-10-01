package com.JavaTraining.BaiTap_RS.timetableagent.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentActionStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqApproveTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqCreateTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqExecuteTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentActionStateDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.service.TimetableAgentActionStateService;
import com.JavaTraining.BaiTap_RS.timetableagent.service.TimetableAgentApprovalService;
import com.JavaTraining.BaiTap_RS.timetableagent.service.TimetableAgentExecutionService;
import com.JavaTraining.BaiTap_RS.timetableagent.service.TimetableAgentOrchestrator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v4/timetable-agent")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
public class TimetableAgentController {

    private final TimetableAgentOrchestrator orchestrator;
    private final TimetableAgentApprovalService approvalService;
    private final TimetableAgentExecutionService executionService;
    private final TimetableAgentActionStateService actionStateService;

    @PostMapping("/proposals")
    @ApiMessage("Tạo bản đề xuất thời khóa biểu")
    public ResponseEntity<ResTimetableAgentProposalDTO> create(
            @Valid @RequestBody ReqCreateTimetableAgentProposalDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orchestrator.create(requireActor(), request));
    }

    @GetMapping("/proposals/{proposalId}")
    @ApiMessage("Lấy bản đề xuất thời khóa biểu")
    public ResTimetableAgentProposalDTO getProposal(@PathVariable @Positive Long proposalId) {
        return orchestrator.get(requireActor(), proposalId);
    }

    @PostMapping("/proposals/{proposalId}/approve")
    @ApiMessage("Duyệt bản đề xuất thời khóa biểu")
    public ResTimetableAgentProposalDTO approve(@PathVariable @Positive Long proposalId,
            @Valid @RequestBody ReqApproveTimetableAgentProposalDTO request) {
        return approvalService.approve(requireActor(), proposalId, request);
    }

    @PostMapping("/proposals/{proposalId}/execute")
    @ApiMessage("Thực thi lưu bản nháp thời khóa biểu")
    public ResponseEntity<ResTimetableAgentActionStateDTO> execute(@PathVariable @Positive Long proposalId,
            @Valid @RequestBody ReqExecuteTimetableAgentProposalDTO request,
            @RequestHeader(name = "Idempotency-Key") @NotBlank String idempotencyKey) {
        ResTimetableAgentActionStateDTO response = executionService.execute(requireActor(), proposalId,
                idempotencyKey, request);
        return ResponseEntity.status(response.status() == TimetableAgentActionStatus.PENDING
                ? HttpStatus.ACCEPTED : HttpStatus.OK).body(response);
    }

    @GetMapping("/actions/{actionId}")
    @ApiMessage("Lấy trạng thái thao tác thời khóa biểu")
    public ResponseEntity<ResTimetableAgentActionStateDTO> getAction(@PathVariable @Positive Long actionId) {
        ResTimetableAgentActionStateDTO response = actionStateService.get(requireActor(), actionId);
        return ResponseEntity.status(response.status()
                == TimetableAgentActionStatus.PENDING
                        ? HttpStatus.ACCEPTED : HttpStatus.OK)
                .body(response);
    }

    @GetMapping("/actions/by-key")
    @ApiMessage("Tra cứu thao tác theo mã chống lặp")
    public ResponseEntity<ResTimetableAgentActionStateDTO> getActionByKey(
            @RequestParam @NotBlank String key) {
        ResTimetableAgentActionStateDTO response = actionStateService.getByKey(requireActor(), key);
        return ResponseEntity.status(response.status()
                == TimetableAgentActionStatus.PENDING
                        ? HttpStatus.ACCEPTED : HttpStatus.OK)
                .body(response);
    }

    private Long requireActor() {
        Long actorId = AuditContext.currentUserId();
        if (actorId == null) {
            throw new com.JavaTraining.BaiTap_RS.common.error.AppException(HttpStatus.UNAUTHORIZED,
                    "Authenticated user is required.");
        }
        return actorId;
    }
}
