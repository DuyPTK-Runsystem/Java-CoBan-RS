package com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response;

import java.util.List;

public record ResEnrollmentMutationDTO(
        List<ResEnrollmentDTO> enrollments,
        List<ResCapacityWarningDTO> warnings) {
}
