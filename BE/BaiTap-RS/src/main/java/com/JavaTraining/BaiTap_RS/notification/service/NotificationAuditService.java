package com.JavaTraining.BaiTap_RS.notification.service;

import java.util.Map;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.audit.domain.entity.AuditLog;
import com.JavaTraining.BaiTap_RS.common.audit.repository.AuditLogRepository;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class NotificationAuditService {

    private static final String KEY_STATUS = "status";

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public NotificationAuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public void auditCreateDraft(Long actorUserId, Notification notification) {
        saveAudit(
                actorUserId,
                "NOTIFICATION_CREATE_DRAFT",
                notification.getId().toString(),
                null,
                Map.of(
                        "id", notification.getId(),
                        "title", notification.getTitle(),
                        "audienceType", notification.getAudienceType().name(),
                        KEY_STATUS, notification.getStatus().name()));
    }

    public void auditPublish(Long actorUserId, Notification notification, int recipientCount) {
        saveAudit(
                actorUserId,
                "NOTIFICATION_PUBLISH",
                notification.getId().toString(),
                Map.of(KEY_STATUS, "DRAFT"),
                Map.of(
                        KEY_STATUS, notification.getStatus().name(),
                        "publishAt", String.valueOf(notification.getPublishAt()),
                        "recipientCount", recipientCount));
    }

    public void auditCancel(Long actorUserId, Notification notification) {
        saveAudit(
                actorUserId,
                "NOTIFICATION_CANCEL",
                notification.getId().toString(),
                Map.of(KEY_STATUS, "DRAFT"),
                Map.of(KEY_STATUS, notification.getStatus().name()));
    }

    public void auditMarkRead(Long actorUserId, Long notificationId) {
        saveAudit(
                actorUserId,
                "NOTIFICATION_MARK_READ",
                notificationId.toString(),
                Map.of("read", false),
                Map.of("read", true));
    }

    private void saveAudit(
            Long actorUserId,
            String action,
            String entityId,
            Map<String, Object> before,
            Map<String, Object> after) {
        auditLogRepository.save(new AuditLog(
                actorUserId,
                action,
                "notification",
                entityId,
                toJson(before),
                toJson(after),
                AuditContext.requestId(),
                AuditContext.ipAddress()));
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
