package com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadEligibilityStatus;
import jakarta.validation.constraints.NotNull;

public record ReqUpdateEligibilityDTO(
                @NotNull(message = "expectedVersion không được để trống") Long expectedVersion,

                LocalDate validFrom,

                LocalDate validTo,

                String evidenceReference,

                TeacherLoadEligibilityStatus status) {
}
