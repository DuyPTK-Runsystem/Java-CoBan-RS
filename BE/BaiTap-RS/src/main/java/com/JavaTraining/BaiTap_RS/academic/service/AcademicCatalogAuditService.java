package com.JavaTraining.BaiTap_RS.academic.service;

import java.util.Map;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.audit.domain.entity.AuditLog;
import com.JavaTraining.BaiTap_RS.common.audit.repository.AuditLogRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class AcademicCatalogAuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AcademicCatalogAuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public void writeAudit(
            String action,
            String entityType,
            Long entityId,
            Map<String, Object> beforeData,
            Map<String, Object> afterData) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        AcademicCatalogAuditService.class,
                        "AcademicCatalogAuditService.writeAudit");
        auditLogRepository.save(new AuditLog(
                AuditContext.currentUserId(),
                action,
                entityType,
                entityId == null ? "pending" : entityId.toString(),
                beforeData == null ? null : auditJson(beforeData),
                afterData == null ? null : auditJson(afterData),
                AuditContext.requestId(),
                AuditContext.ipAddress()));
    }

    private String auditJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException exception) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo dữ liệu audit", exception);
        }
    }
}
