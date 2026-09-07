package com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.response;

import java.util.List;

import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResStudentScoreDTO;

public record ResTransferWithScoresDTO(
        ResEnrollmentMutationDTO transfer,
        List<ResStudentScoreDTO> scores) {
}
