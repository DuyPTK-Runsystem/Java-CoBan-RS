package com.JavaTraining.BaiTap_RS.notification.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqCreateNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationChannel;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

public class NotificationIdempotencyService {

    private static final String IDEMPOTENCY_CONSTRAINT = "uk_notification_idempotency";
    private static final String FINGERPRINT_VERSION = "notification-idempotency-v1";
    private static final String NULL_VALUE = "<null>";

    private final NotificationRepository notificationRepository;

    public NotificationIdempotencyService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification findExisting(String idempotencyKey, String fingerprint, Long actorUserId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        Optional<Notification> existingOpt = notificationRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOpt.isEmpty()) {
            return null;
        }
        Notification existing = existingOpt.get();
        if (!Objects.equals(existing.getSenderId(), actorUserId)) {
            throw new AppException(HttpStatus.CONFLICT, "Idempotency key thuộc về actor khác");
        }
        if (Objects.equals(existing.getIdempotencyFingerprint(), fingerprint)) {
            return existing;
        }
        throw new AppException(HttpStatus.CONFLICT, "Idempotency key trùng lặp với thông báo khác");
    }

    public String buildFingerprint(ReqCreateNotificationDTO request) {
        StringBuilder canonical = new StringBuilder(FINGERPRINT_VERSION);
        appendCanonicalField(canonical, request.getTitle());
        appendCanonicalField(canonical, request.getBody());
        appendCanonicalField(canonical, enumValue(request.getAudienceType()));
        appendCanonicalField(canonical, request.getTargetReference());
        appendCanonicalField(canonical, canonicalRecipientIds(request.getRecipientUserIds()));
        appendCanonicalField(canonical, enumValue(effectiveChannel(request.getChannel())));
        appendCanonicalField(canonical, stringValue(request.getPublishAt()));
        appendCanonicalField(canonical, stringValue(request.getExpiresAt()));
        appendCanonicalField(canonical, request.getSchoolScope());
        return sha256(canonical.toString());
    }

    public Notification save(Notification notification) {
        try {
            return notificationRepository.saveAndFlush(notification);
        } catch (DataIntegrityViolationException exception) {
            if (isIdempotencyConstraintViolation(exception)) {
                throw new AppException(
                        HttpStatus.CONFLICT,
                        "Idempotency key đã được sử dụng đồng thời; vui lòng thử lại",
                        exception);
            }
            throw exception;
        }
    }

    private String canonicalRecipientIds(List<Long> recipientUserIds) {
        if (recipientUserIds == null || recipientUserIds.isEmpty()) {
            return "";
        }
        return recipientUserIds.stream()
                .map(id -> id == null ? NULL_VALUE : String.valueOf(id))
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));
    }

    private NotificationChannel effectiveChannel(NotificationChannel channel) {
        return channel == null ? NotificationChannel.IN_APP : channel;
    }

    private String enumValue(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void appendCanonicalField(StringBuilder canonical, String value) {
        if (value == null) {
            canonical.append("-1:");
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        canonical.append(bytes.length).append(':').append(value);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                hex.append(String.format(Locale.ROOT, "%02x", item));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 không khả dụng", exception);
        }
    }

    private boolean isIdempotencyConstraintViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && IDEMPOTENCY_CONSTRAINT.equalsIgnoreCase(constraintViolation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        String message = exception.getMostSpecificCause() == null
                ? exception.getMessage()
                : exception.getMostSpecificCause().getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains(IDEMPOTENCY_CONSTRAINT);
    }
}
