package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreChangeRequestStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReqFilterScoreChangeRequestDTO {

    private ScoreChangeRequestStatus status;
    private Long scorebookId;
    private Long columnId;
    private Long studentId;
    private Long requestedBy;
    private int page;
    private int size = 10;
}
