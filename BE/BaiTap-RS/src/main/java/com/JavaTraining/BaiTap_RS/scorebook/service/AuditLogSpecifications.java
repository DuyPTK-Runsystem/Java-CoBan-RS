package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.Locale;

import com.JavaTraining.BaiTap_RS.common.audit.domain.entity.AuditLog;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqFilterScoreAuditLogDTO;
import org.springframework.data.jpa.domain.Specification;

@SuppressWarnings({ "PMD.CyclomaticComplexity", "PMD.NPathComplexity" })
final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    /* default */ static Specification<AuditLog> from(ReqFilterScoreAuditLogDTO filter, Long resolvedStudentId) {
        Specification<AuditLog> specification = (root, query, builder) -> builder.conjunction();
        if (filter.getEntityType() != null && !filter.getEntityType().isBlank()) {
            String entityType = filter.getEntityType().trim().toLowerCase(Locale.ROOT);
            specification = specification.and((root, query, builder) ->
                    builder.equal(builder.lower(root.get("entityType")), entityType));
        }
        if (filter.getEntityId() != null && !filter.getEntityId().isBlank()) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("entityId"), filter.getEntityId().trim()));
        }
        if (filter.getAction() != null && !filter.getAction().isBlank()) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("action"), filter.getAction().trim()));
        }
        if (filter.getActorUserId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("actorUserId"), filter.getActorUserId()));
        }
        if (filter.getFromOccurredAt() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("occurredAt"), filter.getFromOccurredAt()));
        }
        if (filter.getToOccurredAt() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("occurredAt"), filter.getToOccurredAt()));
        }
        if (resolvedStudentId != null) {
            String studentPattern = "%\"studentId\":" + resolvedStudentId + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(root.get("beforeData"), studentPattern),
                    builder.like(root.get("afterData"), studentPattern)));
        }
        return specification;
    }
}
