package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReqRejectScoreChangeRequestDTO(
        @NotBlank(message = "Lý do từ chối không được để trống")
        @Size(max = 1000, message = "Lý do từ chối không quá 1000 ký tự") String rejectionReason) {
}
