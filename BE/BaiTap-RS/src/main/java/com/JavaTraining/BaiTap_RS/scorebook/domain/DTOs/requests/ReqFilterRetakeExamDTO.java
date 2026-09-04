package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.RetakeExamStatus;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReqFilterRetakeExamDTO {

    private Long studentId;
    private Long academicYearId;
    private Long subjectId;
    private RetakeExamStatus status;

    @PositiveOrZero
    private int page;

    @Positive
    private int size = 10;
}
