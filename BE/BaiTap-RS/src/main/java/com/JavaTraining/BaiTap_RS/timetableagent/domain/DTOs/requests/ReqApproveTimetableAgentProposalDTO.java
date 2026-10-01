package com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReqApproveTimetableAgentProposalDTO(
        @NotNull @Positive Long proposalVersion,
        @NotBlank String proposalHash) {
}
