package com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReqActivateLibraryPatronDTO(@NotNull @Positive Long userId) {
}
