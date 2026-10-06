package com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record ReqCreateTimetableAgentProposalDTO(
        @NotNull @Positive Long targetRevisionId,
        @NotNull @PositiveOrZero Long expectedVersion,
        @NotEmpty List<@NotNull @Positive Long> classIds,
        @NotNull LocalDate validFrom,
        @NotNull LocalDate validTo,
        @NotEmpty List<@Valid ReqTimetableAgentDemandDTO> demands,
        List<@NotNull @Positive Long> lockedEntryIds,
        String preferences,
        String userRequest) {
}
