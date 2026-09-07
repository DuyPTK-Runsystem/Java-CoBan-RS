package com.JavaTraining.BaiTap_RS.user.domain.DTOs.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResLoginUserDTO(
        @JsonProperty("access_token")
        String accessToken,
        ResUserDTO user) {
}
