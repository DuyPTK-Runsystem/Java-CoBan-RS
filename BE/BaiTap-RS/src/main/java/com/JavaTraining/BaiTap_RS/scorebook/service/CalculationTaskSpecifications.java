package com.JavaTraining.BaiTap_RS.scorebook.service;

import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqFilterCalculationTaskDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.CalculationTask;
import org.springframework.data.jpa.domain.Specification;

final class CalculationTaskSpecifications {

    private CalculationTaskSpecifications() {
    }

    /* default */ static Specification<CalculationTask> from(
            ReqFilterCalculationTaskDTO filter, Long resolvedStudentId) {
        Specification<CalculationTask> specification = (root, query, builder) -> builder.conjunction();
        if (filter.getStatus() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), filter.getStatus()));
        }
        if (resolvedStudentId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("studentId"), resolvedStudentId));
        }
        if (filter.getAcademicYearId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("academicYearId"), filter.getAcademicYearId()));
        }
        return specification;
    }
}
