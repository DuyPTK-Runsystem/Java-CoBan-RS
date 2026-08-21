package com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response;

import java.time.LocalDateTime;

public record ResTransferHistoryDTO(
        Long transferId,
        Long fromClassId,
        Long toClassId,
        LocalDateTime effectiveAt,
        String reason,
        Long approvedBy) {
}
