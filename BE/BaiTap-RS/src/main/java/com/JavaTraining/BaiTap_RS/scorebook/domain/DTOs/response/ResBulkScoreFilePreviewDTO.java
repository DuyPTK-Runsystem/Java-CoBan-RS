package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response;

import java.util.List;

public record ResBulkScoreFilePreviewDTO(
        Long assessmentColumnId,
        String fileName,
        ResBulkScoreFileSummaryDTO summary,
        List<ResBulkScoreFileRowDTO> rows,
        List<ResBulkScoreFileItemDTO> items) {
}
