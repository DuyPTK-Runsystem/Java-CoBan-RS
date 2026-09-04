package com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response;

import java.util.List;

public record ResStudentEnrollmentHistoryDTO(
        ResEnrollmentDTO enrollment,
        List<ResTransferHistoryDTO> transfers) {
}
