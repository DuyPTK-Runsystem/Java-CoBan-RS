package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests;

import java.math.BigDecimal;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReqCreateScoreChangeRequestDTO(
        @NotNull(message = "Cột điểm không được để trống") Long assessmentColumnId,
        @NotNull(message = "Học sinh không được để trống") Long studentId,
        @NotNull(message = "Trạng thái điểm đề xuất không được để trống") ScoreStatus proposedStatus,
        BigDecimal proposedValue,
        @NotBlank(message = "Lý do sửa điểm không được để trống")
        @Size(max = 1000, message = "Lý do sửa điểm không quá 1000 ký tự") String reason) {
}
