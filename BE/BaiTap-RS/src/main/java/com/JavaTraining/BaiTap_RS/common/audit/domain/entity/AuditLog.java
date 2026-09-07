package com.JavaTraining.BaiTap_RS.common.audit.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// NFR-AUDITABILITY-001: transfer phải lưu before/after và request context.
@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id", nullable = false)
    private Long id;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    @Column(name = "before_data", columnDefinition = "JSON")
    private String beforeData;

    @Column(name = "after_data", columnDefinition = "JSON")
    private String afterData;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    public AuditLog(
            Long actorUserId,
            String action,
            String entityType,
            String entityId,
            String beforeData,
            String afterData,
            String requestId,
            String ipAddress) {
        this.actorUserId = actorUserId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.beforeData = beforeData;
        this.afterData = afterData;
        this.requestId = requestId;
        this.ipAddress = ipAddress;
    }

    @jakarta.persistence.PrePersist
    /* default */ void onCreate() {
        occurredAt = LocalDateTime.now();
    }
}
