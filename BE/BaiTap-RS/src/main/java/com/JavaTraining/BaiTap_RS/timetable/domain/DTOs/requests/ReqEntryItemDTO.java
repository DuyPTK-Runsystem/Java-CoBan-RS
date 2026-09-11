package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record ReqEntryItemDTO(
                Long entryId,

                @NotNull(message = "Phân công không được để trống") Long assignmentId,

                @NotNull(message = "Tiết học (period) không được để trống") Long periodId,

                Long functionalRoomId,

                @NotNull(message = "Ngày bắt đầu áp dụng không được để trống") LocalDate validFrom,

                @NotNull(message = "Ngày kết thúc áp dụng không được để trống") LocalDate validTo) {
}
