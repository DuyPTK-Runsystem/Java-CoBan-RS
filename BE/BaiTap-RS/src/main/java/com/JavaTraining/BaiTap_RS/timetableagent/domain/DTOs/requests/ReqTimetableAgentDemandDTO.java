package com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReqTimetableAgentDemandDTO(
        @NotNull @Positive Long assignmentId,
        @NotNull @Positive Integer periodsPerWeek) {
}
