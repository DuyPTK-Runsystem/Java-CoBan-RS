package com.JavaTraining.BaiTap_RS.scorebook.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqFilterScoreAuditLogDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScoreAuditLogDTO;
import com.JavaTraining.BaiTap_RS.scorebook.service.ScoreAuditLogService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2/scorebooks/audit-logs")
public class ScoreAuditLogController {

    private final ScoreAuditLogService scoreAuditLogService;

    public ScoreAuditLogController(ScoreAuditLogService scoreAuditLogService) {
        this.scoreAuditLogService = scoreAuditLogService;
    }

    @GetMapping
    @ApiMessage("Tra cứu audit log điểm")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')")
    public Page<ResScoreAuditLogDTO> find(@Valid @ModelAttribute ReqFilterScoreAuditLogDTO filter) {
        return scoreAuditLogService.findLogs(filter);
    }
}
