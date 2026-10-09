package com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReqPaymentDTO(@NotBlank @Size(max = 200) String reference) {
}
