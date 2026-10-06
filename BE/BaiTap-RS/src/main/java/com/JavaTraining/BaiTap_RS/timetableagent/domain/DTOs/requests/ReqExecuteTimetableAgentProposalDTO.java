package com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReqExecuteTimetableAgentProposalDTO(
        @NotNull @Positive Long proposalVersion) {
}
