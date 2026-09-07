package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReqReopenSemesterDTO(@NotBlank @Size(max = 500) String reason) {
}
