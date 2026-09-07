package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response;

import java.util.List;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;

public record ResStudentScoreGridDTO(
                Long scorebookId,
                Long classSubjectId,
                ScorebookStatus scorebookStatus,
                List<ResScoreGridColumnDTO> columns,
                int page,
                int size,
                long totalElements,
                int totalPages,
                List<ResScoreGridStudentRowDTO> students) {
}
