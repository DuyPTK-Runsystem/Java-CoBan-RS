package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReqDispatchNotificationDTO(
        @NotBlank(message = "Checkpoint code is required")
        @Size(max = 20, message = "Checkpoint code must not exceed 20 characters")
        String checkpointCode) {
}
