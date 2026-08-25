package com.JavaTraining.BaiTap_RS.scorebook.service;

import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqFilterScoreChangeRequestDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreChangeRequest;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

final class ScoreChangeRequestSpecifications {

    private ScoreChangeRequestSpecifications() {
    }

    /* default */ static Specification<ScoreChangeRequest> from(ReqFilterScoreChangeRequestDTO filter) {
        Specification<ScoreChangeRequest> specification = (root, query, builder) -> builder.conjunction();
        if (filter.getStatus() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), filter.getStatus()));
        }
        if (filter.getColumnId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("assessmentColumnId"), filter.getColumnId()));
        }
        if (filter.getStudentId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("studentId"), filter.getStudentId()));
        }
        if (filter.getRequestedBy() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("requestedBy"), filter.getRequestedBy()));
        }
        if (filter.getScorebookId() != null) {
            specification = specification.and((root, query, builder) -> {
                Subquery<Long> columnQuery = query.subquery(Long.class);
                Root<AssessmentColumn> column = columnQuery.from(AssessmentColumn.class);
                columnQuery.select(column.get("id"));
                columnQuery.where(
                        builder.equal(column.get("id"), root.get("assessmentColumnId")),
                        builder.equal(column.get("scorebookId"), filter.getScorebookId()));
                return builder.exists(columnQuery);
            });
        }
        return specification;
    }
}
