package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ReqUpdateTimetableEntriesDTO(
        @NotNull(message = "expectedVersion không được để trống")
        Long expectedVersion,

        @Valid
        List<ReqEntryItemDTO> upserts,

        List<Long> deletedEntryIds
) {}
