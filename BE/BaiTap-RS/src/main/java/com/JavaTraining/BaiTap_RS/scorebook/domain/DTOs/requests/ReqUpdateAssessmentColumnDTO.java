package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests;

import jakarta.validation.constraints.Size;

public record ReqUpdateAssessmentColumnDTO(
        @Size(max = 100) String columnName) {
}
