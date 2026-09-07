package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response;

import java.time.LocalDate;

public record ResSemesterCompletenessDecisionDTO(
        Long semesterId,
        LocalDate checkpointDate,
        String checkpointCode,
        String decision) {
}
