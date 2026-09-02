package com.JavaTraining.BaiTap_RS.scorebook.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqCreateAssessmentColumnDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqCreateScorebookDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqUpdateAssessmentColumnDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqUpsertSkillWeightConfigDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResAssessmentColumnDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScorebookDTO;
import com.JavaTraining.BaiTap_RS.scorebook.service.ScorebookService;
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
@RequestMapping("/api/v2")
@SuppressWarnings("PMD.GuardLogStatement")
public class ScorebookController {

    private static final String OFFICE_ROLES = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')";
    private static final String SCOREBOOK_ROLES = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')";
    private static final String SCOREBOOK_ID = "scorebookId";
    private static final String COLUMN_ID = "columnId";

    private final ScorebookService scorebookService;

    public ScorebookController(ScorebookService scorebookService) {
        this.scorebookService = scorebookService;
    }

    @PostMapping("/scorebooks")
    @ApiMessage("Tạo sổ điểm")
    @PreAuthorize(OFFICE_ROLES)
    public ResponseEntity<ResScorebookDTO> createScorebook(
            @Valid @RequestBody ReqCreateScorebookDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        ScorebookController.class,
                        "ScorebookController.createScorebook");
        return ResponseEntity.status(HttpStatus.CREATED).body(scorebookService.createScorebook(request));
    }

    @GetMapping("/scorebooks/{scorebookId}")
    @ApiMessage("Lấy sổ điểm")
    @PreAuthorize(SCOREBOOK_ROLES)
    public ResScorebookDTO getScorebook(@PathVariable(SCOREBOOK_ID) @Positive Long scorebookId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScorebookController.class,
                "ScorebookController.getScorebook");
        return scorebookService.getScorebook(scorebookId);
    }

    @GetMapping("/scorebooks/by-class-subject/{classSubjectId}")
    @ApiMessage("Lấy sổ điểm theo lớp-môn")
    @PreAuthorize(SCOREBOOK_ROLES)
    public ResScorebookDTO getScorebookByClassSubject(
            @PathVariable("classSubjectId") @Positive Long classSubjectId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScorebookController.class,
                "ScorebookController.getScorebookByClassSubject");
        return scorebookService.getScorebookByClassSubject(classSubjectId);
    }

    @PostMapping("/scorebooks/{scorebookId}/open")
    @ApiMessage("Mở sổ điểm")
    @PreAuthorize(SCOREBOOK_ROLES)
    public ResScorebookDTO openScorebook(@PathVariable(SCOREBOOK_ID) @Positive Long scorebookId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScorebookController.class,
                "ScorebookController.openScorebook");
        return scorebookService.openScorebook(scorebookId);
    }

    @PostMapping("/scorebooks/{scorebookId}/columns")
    @ApiMessage("Tạo cột điểm")
    @PreAuthorize(SCOREBOOK_ROLES)
    public ResAssessmentColumnDTO addColumn(
            @PathVariable(SCOREBOOK_ID) @Positive Long scorebookId,
            @Valid @RequestBody ReqCreateAssessmentColumnDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        ScorebookController.class,
                        "ScorebookController.addColumn");
        return scorebookService.addColumn(scorebookId, request);
    }

    @PutMapping("/assessment-columns/{columnId}")
    @ApiMessage("Cập nhật cột điểm")
    @PreAuthorize(SCOREBOOK_ROLES)
    public ResAssessmentColumnDTO updateColumn(
            @PathVariable(COLUMN_ID) @Positive Long columnId,
            @Valid @RequestBody ReqUpdateAssessmentColumnDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        ScorebookController.class,
                        "ScorebookController.updateColumn");
        return scorebookService.updateColumn(columnId, request);
    }

    @DeleteMapping("/assessment-columns/{columnId}")
    @ApiMessage("Vô hiệu hóa cột điểm")
    @PreAuthorize(SCOREBOOK_ROLES)
    public ResponseEntity<Void> deactivateColumn(@PathVariable(COLUMN_ID) @Positive Long columnId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScorebookController.class,
                "ScorebookController.deactivateColumn");
        scorebookService.deactivateColumn(columnId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/scorebooks/{scorebookId}/skill-weight")
    @ApiMessage("Cấu hình trọng số môn kỹ năng")
    @PreAuthorize(SCOREBOOK_ROLES)
    public ResScorebookDTO upsertSkillWeight(
            @PathVariable(SCOREBOOK_ID) @Positive Long scorebookId,
            @Valid @RequestBody ReqUpsertSkillWeightConfigDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        ScorebookController.class,
                        "ScorebookController.upsertSkillWeight");
        return scorebookService.upsertSkillWeight(scorebookId, request);
    }

    @PostMapping("/scorebooks/{scorebookId}/publish")
    @ApiMessage("Công bố sổ điểm")
    @PreAuthorize(SCOREBOOK_ROLES)
    public ResScorebookDTO publishScorebook(@PathVariable(SCOREBOOK_ID) @Positive Long scorebookId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScorebookController.class,
                "ScorebookController.publishScorebook");
        return scorebookService.publishScorebook(scorebookId);
    }
}
