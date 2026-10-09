package com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReqReasonDTO(@NotBlank @Size(max = 500) String reason) {
}
