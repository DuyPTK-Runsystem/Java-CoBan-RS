package com.JavaTraining.BaiTap_RS.library.common.service;

import java.util.Map;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.audit.domain.entity.AuditLog;
import com.JavaTraining.BaiTap_RS.common.audit.repository.AuditLogRepository;
import com.JavaTraining.BaiTap_RS.library.patron.exception.LibraryPatronException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LibraryOperationAuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public void record(String action, String type, Long id, Map<String, Object> before, Map<String, Object> after) {
        record(AuditContext.currentUserId(), action, type, id, before, after);
    }

    public void record(Long actorId, String action, String type, Long id,
            Map<String, Object> before, Map<String, Object> after) {
        auditLogRepository.save(new AuditLog(actorId, action, type,
                id == null ? "pending" : id.toString(), json(before), json(after),
                AuditContext.requestId(), AuditContext.ipAddress()));
    }

    private String json(Map<String, Object> value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new LibraryPatronException(HttpStatus.INTERNAL_SERVER_ERROR, "LIBRARY_AUDIT_FAILED",
                    "Không thể ghi lịch sử thao tác", exception);
        }
    }
}
