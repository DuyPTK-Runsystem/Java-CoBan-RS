package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.SessionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** @param dayOfWeek ISO weekday (1=Monday through 7=Sunday); null allows a specific date. */
public record ReqUpdateUnavailabilityDTO(
                @NotNull(message = "expectedVersion không được để trống") Long expectedVersion,

                @Min(1) @Max(7) Integer dayOfWeek,

                LocalDate specificDate,

                @NotNull(message = "validFrom không được để trống") LocalDate validFrom,

                @NotNull(message = "validTo không được để trống") LocalDate validTo,

                @NotNull(message = "session không được để trống") SessionType session,

                @NotBlank(message = "periodIndexes không được để trống") String periodIndexes,

                String note) {
}
