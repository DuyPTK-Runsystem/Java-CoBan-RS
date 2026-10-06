package com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TimetableAgentProposalEntryDTO(
        @NotNull @Positive Long assignmentId,
        @NotNull @Positive Long periodId,
        Long functionalRoomId,
        @NotNull LocalDate validFrom,
        @NotNull LocalDate validTo) {
}
