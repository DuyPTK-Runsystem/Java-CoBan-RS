package com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response;

import java.util.List;

public record ResTransferScoreSubjectDTO(
        Long subjectId,
        String subjectCode,
        String subjectName,
        List<ResTransferSourceScoreDTO> sourceEvidence,
        List<ResTransferTargetColumnDTO> targetColumns) {
}
