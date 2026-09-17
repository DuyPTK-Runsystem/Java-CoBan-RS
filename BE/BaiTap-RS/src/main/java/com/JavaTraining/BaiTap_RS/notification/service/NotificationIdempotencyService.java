package com.JavaTraining.BaiTap_RS.notification.service;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqCreateNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

public class NotificationIdempotencyService {

    private static final String IDEMPOTENCY_CONSTRAINT = "uk_notification_idempotency";

    private final NotificationRepository notificationRepository;
    private final NotificationFingerprintBuilder fingerprintBuilder;

    public NotificationIdempotencyService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
        this.fingerprintBuilder = new NotificationFingerprintBuilder();
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
        return fingerprintBuilder.build(request);
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
