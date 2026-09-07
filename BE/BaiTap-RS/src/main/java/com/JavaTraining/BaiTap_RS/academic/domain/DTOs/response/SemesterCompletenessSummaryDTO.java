package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response;

import java.util.List;

public record SemesterCompletenessSummaryDTO(
                boolean complete,
                int missingKtdkCount,
                int invalidKtckCount,
                int missingSkillColumnsCount,
                int unenteredScoreCount,
                int studentWithoutScoreDataCount,
                int unpublishedScorebookCount,
                int pendingScoreChangeRequestCount,
                List<String> details) {
}
