package com.JavaTraining.BaiTap_RS.academic.domain.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@SuppressWarnings("PMD.TooManyFields")
@Table(name = "semester_completeness_notification", uniqueConstraints = @UniqueConstraint(
        name = "uk_sem_notif_chk_recip", columnNames = {
        "semester_id", "checkpoint_code", "recipient_email", "notification_channel" }))
public class SemesterCompletenessNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "semester_id", nullable = false)
    private Long semesterId;

    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "checkpoint_code", nullable = false, length = 20)
    private String checkpointCode;

    @Column(name = "recipient_email", nullable = false, length = 150)
    private String recipientEmail;

    @Column(name = "recipient_role", nullable = false, length = 50)
    private String recipientRole;

    @Column(name = "recipient_teacher_id")
    private Long recipientTeacherId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_channel", nullable = false, length = 20)
    private NotificationChannel notificationChannel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "body_content", nullable = false, columnDefinition = "TEXT")
    private String bodyContent;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public SemesterCompletenessNotification(
            Long semesterId,
            Long reportId,
            String checkpointCode,
            String recipientEmail,
            String recipientRole,
            Long recipientTeacherId,
            NotificationChannel notificationChannel,
            NotificationStatus status,
            String subject,
            String bodyContent) {
        this.semesterId = semesterId;
        this.reportId = reportId;
        this.checkpointCode = checkpointCode;
        this.recipientEmail = recipientEmail;
        this.recipientRole = recipientRole;
        this.recipientTeacherId = recipientTeacherId;
        this.notificationChannel = notificationChannel != null ? notificationChannel : NotificationChannel.EMAIL;
        this.status = status != null ? status : NotificationStatus.PENDING;
        this.subject = subject;
        this.bodyContent = bodyContent;
        this.attemptCount = 0;
    }

    @PrePersist
    /* default */ void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    /* default */ void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
