package com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReqReservationDTO(@NotNull @Positive Long bookId) {
}
