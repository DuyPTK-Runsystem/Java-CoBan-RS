package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.Map;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.audit.domain.entity.AuditLog;
import com.JavaTraining.BaiTap_RS.common.audit.repository.AuditLogRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ScorebookAuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public ScorebookAuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public void writeAudit(
            String action,
            String entityType,
            Long entityId,
            Map<String, Object> beforeData,
            Map<String, Object> afterData) {
        auditLogRepository.save(new AuditLog(
                AuditContext.currentUserId(),
                action,
                entityType,
                entityId == null ? "pending" : entityId.toString(),
                toJson(beforeData),
                toJson(afterData),
                AuditContext.requestId(),
                AuditContext.ipAddress()));
    }

    private String toJson(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException exception) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo dữ liệu audit", exception);
        }
    }
}
