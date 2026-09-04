package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record ReqBulkUpsertStudentScoreDTO(
        @NotEmpty(message = "Danh sách điểm không được rỗng")
        List<@Valid ReqBulkScoreItemDTO> items
) {
}
