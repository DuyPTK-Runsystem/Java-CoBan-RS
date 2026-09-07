package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests;

import java.math.BigDecimal;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReqBulkScoreItemDTO(
        @Positive(message = "ID học sinh phải là số dương")
        Long studentId,

        String studentCode,

        @NotNull(message = "Trạng thái điểm không được để trống")
        ScoreStatus scoreStatus,

        BigDecimal scoreValue,

        @Size(max = 500, message = "Ghi chú không quá 500 ký tự")
        String note,

        Long expectedVersion
) {

    @AssertTrue(message = "Phải cung cấp studentId hoặc studentCode")
    public boolean isStudentIdentifierProvided() {
        return studentId != null || (studentCode != null && !studentCode.isBlank());
    }
}
