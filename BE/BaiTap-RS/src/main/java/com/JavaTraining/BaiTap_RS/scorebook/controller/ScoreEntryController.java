package com.JavaTraining.BaiTap_RS.scorebook.controller;

import java.util.List;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqBulkUpsertStudentScoreDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqUpsertStudentScoreDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentScoreDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentScoreGridDTO;
import com.JavaTraining.BaiTap_RS.scorebook.service.ScoreEntryService;
import com.JavaTraining.BaiTap_RS.scorebook.service.ScoreGridService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
public class ScoreEntryController {

    private static final String SCOREBOOK_ROLES = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')";
    private static final String SCOREBOOK_ID = "scorebookId";
    private static final String COLUMN_ID = "columnId";
    private static final String STUDENT_ID = "studentId";
    private static final String STUDENT_CODE = "studentCode";

    private final ScoreEntryService scoreEntryService;
    private final ScoreGridService scoreGridService;

    public ScoreEntryController(
            ScoreEntryService scoreEntryService,
            ScoreGridService scoreGridService) {
        this.scoreEntryService = scoreEntryService;
        this.scoreGridService = scoreGridService;
    }

    @GetMapping("/scorebooks/{scorebookId}/score-entries")
    @ApiMessage("Đọc bảng điểm")
    @PreAuthorize(SCOREBOOK_ROLES)
    public ResStudentScoreGridDTO getScoreGrid(
            @PathVariable(SCOREBOOK_ID) @Positive Long scorebookId,
            @RequestParam(value = "page", defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(value = "size", defaultValue = "10") @Positive int size) {
        return scoreGridService.getScoreGrid(scorebookId, page, size);
    }

    @PutMapping("/assessment-columns/{columnId}/students/{studentId}/score")
    @ApiMessage("Nhập/cập nhật điểm học sinh")
    @PreAuthorize(SCOREBOOK_ROLES)
    public ResStudentScoreDTO upsertScore(
            @PathVariable(COLUMN_ID) @Positive Long columnId,
            @PathVariable(STUDENT_ID) @Positive Long studentId,
            @Valid @RequestBody ReqUpsertStudentScoreDTO request) {
        return scoreEntryService.upsertSingleScore(columnId, studentId, request);
    }

    @PutMapping("/assessment-columns/{columnId}/students/by-code/{studentCode}/score")
    @ApiMessage("Nhập/cập nhật điểm học sinh theo mã")
    @PreAuthorize(SCOREBOOK_ROLES)
    public ResStudentScoreDTO upsertScoreByCode(
            @PathVariable(COLUMN_ID) @Positive Long columnId,
            @PathVariable(STUDENT_CODE) String studentCode,
            @Valid @RequestBody ReqUpsertStudentScoreDTO request) {
        return scoreEntryService.upsertSingleScoreByCode(columnId, studentCode, request);
    }

    @PostMapping("/assessment-columns/{columnId}/scores/bulk")
    @ApiMessage("Nhập điểm hàng loạt")
    @PreAuthorize(SCOREBOOK_ROLES)
    public List<ResStudentScoreDTO> bulkUpsertScores(
            @PathVariable(COLUMN_ID) @Positive Long columnId,
            @Valid @RequestBody ReqBulkUpsertStudentScoreDTO request) {
        return scoreEntryService.bulkUpsertScores(columnId, request);
    }
}
