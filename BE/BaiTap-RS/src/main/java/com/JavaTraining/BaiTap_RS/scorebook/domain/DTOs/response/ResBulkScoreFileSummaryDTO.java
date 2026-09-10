package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response;

public record ResBulkScoreFileSummaryDTO(
        int totalRows,
        int validRows,
        int skippedRows,
        int errorRows,
        int newScores,
        int updatedScores) {
}
