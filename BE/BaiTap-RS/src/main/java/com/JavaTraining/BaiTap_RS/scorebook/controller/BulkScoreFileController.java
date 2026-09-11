package com.JavaTraining.BaiTap_RS.scorebook.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResBulkScoreFilePreviewDTO;
import com.JavaTraining.BaiTap_RS.scorebook.service.BulkScoreFileService;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
@RequestMapping("/api/v2/assessment-columns")
@SuppressWarnings("PMD.GuardLogStatement")
public class BulkScoreFileController {

    private static final String SCOREBOOK_ROLES = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')";
    private static final String COLUMN_ID = "columnId";
    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final BulkScoreFileService service;

    public BulkScoreFileController(BulkScoreFileService service) {
        this.service = service;
    }

    @GetMapping(
            value = "/{columnId}/scores/bulk-template",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @ApiMessage("Tải file mẫu nhập điểm hàng loạt")
    @PreAuthorize(SCOREBOOK_ROLES)
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable(COLUMN_ID) @Positive Long columnId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                BulkScoreFileController.class,
                "BulkScoreFileController.downloadTemplate");
        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=bulk-score-template-" + columnId + ".xlsx")
                .body(service.createTemplate(columnId));
    }

    @PostMapping(value = "/{columnId}/scores/bulk/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiMessage("Xem trước file nhập điểm hàng loạt")
    @PreAuthorize(SCOREBOOK_ROLES)
    public ResponseEntity<ResBulkScoreFilePreviewDTO> preview(
            @PathVariable(COLUMN_ID) @Positive Long columnId,
            @RequestParam("file") MultipartFile file) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                BulkScoreFileController.class,
                "BulkScoreFileController.preview");
        return ResponseEntity.status(HttpStatus.OK).body(service.preview(columnId, file));
    }
}
