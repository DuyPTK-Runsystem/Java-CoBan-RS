package com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record ReqOverdueFineRunDTO(@NotNull LocalDate runDate) {
}
