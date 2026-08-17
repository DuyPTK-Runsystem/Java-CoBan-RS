package com.JavaTraining.BaiTap_RS.user.domain.DTOs.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResUserDTO(
        Long id,
        String username,
        @JsonProperty("created_at")
        Instant createdAt,
        @JsonProperty("updated_at")
        Instant updatedAt,
        @JsonProperty("created_by")
        String createdBy,
        @JsonProperty("updated_by")
        String updatedBy) {
}
