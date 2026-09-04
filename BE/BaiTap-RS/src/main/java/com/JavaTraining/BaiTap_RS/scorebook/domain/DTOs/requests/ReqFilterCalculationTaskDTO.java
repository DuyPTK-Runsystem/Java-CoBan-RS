package com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests;

import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationTaskStatus;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReqFilterCalculationTaskDTO {

    private CalculationTaskStatus status;
    private Long studentId;
    private String studentCode;
    private Long academicYearId;

    @PositiveOrZero
    private int page;

    @Positive
    private int size = 10;
}
