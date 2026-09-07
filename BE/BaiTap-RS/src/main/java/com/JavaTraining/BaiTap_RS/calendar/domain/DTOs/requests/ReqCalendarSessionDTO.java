package com.JavaTraining.BaiTap_RS.calendar.domain.DTOs.requests;

import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSessionPeriod;
import com.JavaTraining.BaiTap_RS.calendar.domain.entity.CalendarSessionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReqCalendarSessionDTO(
        @NotNull CalendarSessionPeriod sessionPeriod,
        @NotNull CalendarSessionStatus sessionStatus,
        @Size(max = 500) String reason) {
}
