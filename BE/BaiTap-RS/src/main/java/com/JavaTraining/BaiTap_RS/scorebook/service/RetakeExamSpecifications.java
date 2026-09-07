package com.JavaTraining.BaiTap_RS.scorebook.service;

import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqFilterRetakeExamDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.RetakeExam;
import org.springframework.data.jpa.domain.Specification;

final class RetakeExamSpecifications {

    private RetakeExamSpecifications() {
    }

    /* default */ static Specification<RetakeExam> from(ReqFilterRetakeExamDTO filter) {
        Specification<RetakeExam> specification = (root, query, builder) -> builder.conjunction();
        if (filter.getStudentId() != null) {
            specification = specification
                    .and((root, query, builder) -> builder.equal(root.get("studentId"), filter.getStudentId()));
        }
        if (filter.getAcademicYearId() != null) {
            specification = specification.and(
                    (root, query, builder) -> builder.equal(root.get("academicYearId"), filter.getAcademicYearId()));
        }
        if (filter.getSubjectId() != null) {
            specification = specification
                    .and((root, query, builder) -> builder.equal(root.get("subjectId"), filter.getSubjectId()));
        }
        if (filter.getStatus() != null) {
            specification = specification
                    .and((root, query, builder) -> builder.equal(root.get("status"), filter.getStatus()));
        }
        return specification;
    }
}
