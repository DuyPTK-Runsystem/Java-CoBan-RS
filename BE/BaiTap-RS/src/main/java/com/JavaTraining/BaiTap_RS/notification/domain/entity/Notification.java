package com.JavaTraining.BaiTap_RS.notification.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id", nullable = false)
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 30)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NotificationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false, length = 30)
    private NotificationAudienceType audienceType;

    @Column(name = "target_reference", length = 255)
    private String targetReference;

    @Column(name = "school_scope", nullable = false, length = 100)
    private String schoolScope = "DEFAULT_SCHOOL";

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "publish_at")
    private LocalDateTime publishAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @jakarta.persistence.Embedded
    private NotificationIdempotency idempotency = new NotificationIdempotency();

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Notification(
            String title,
            String body,
            NotificationAudienceType audienceType,
            String targetReference,
            Long senderId,
            String schoolScope) {
        this.title = title;
        this.body = body;
        this.audienceType = audienceType;
        this.targetReference = targetReference;
        this.senderId = senderId;
        if (schoolScope != null && !schoolScope.isBlank()) {
            this.schoolScope = schoolScope;
        }
        this.status = NotificationStatus.DRAFT;
        this.channel = NotificationChannel.IN_APP;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getIdempotencyKey() {
        return idempotency == null ? null : idempotency.getKey();
    }

    public void setIdempotencyKey(String idempotencyKey) {
        ensureIdempotency().setKey(idempotencyKey);
    }

    public String getIdempotencyFingerprint() {
        return idempotency == null ? null : idempotency.getFingerprint();
    }

    public void setIdempotencyFingerprint(String idempotencyFingerprint) {
        ensureIdempotency().setFingerprint(idempotencyFingerprint);
    }

    private NotificationIdempotency ensureIdempotency() {
        if (idempotency == null) {
            idempotency = new NotificationIdempotency();
        }
        return idempotency;
    }
}
