package com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response;

import java.time.LocalDate;

public record ResTimetableAgentExistingEntryDTO(
        Long entryId,
        Long assignmentId,
        Long periodId,
        Long functionalRoomId,
        LocalDate validFrom,
        LocalDate validTo) {
}
