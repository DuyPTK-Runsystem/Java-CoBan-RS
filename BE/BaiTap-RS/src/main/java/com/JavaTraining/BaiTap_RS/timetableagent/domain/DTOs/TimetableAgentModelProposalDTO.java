package com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TimetableAgentModelProposalDTO(
        @NotBlank String schemaVersion,
        @NotNull TimetableAgentModelProposalStatus status,
        @NotBlank String snapshotId,
        @NotNull List<@Valid TimetableAgentProposalEntryDTO> entries,
        @NotNull List<@Valid TimetableAgentUnresolvedConstraintDTO> unresolvedConstraints,
        @NotNull String explanation) {
}
