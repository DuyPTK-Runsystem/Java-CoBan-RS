package com.JavaTraining.BaiTap_RS.notification.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqCreateNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationAudienceType;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationChannel;

public class NotificationFingerprintBuilder {

    private static final String FINGERPRINT_VERSION = "notification-idempotency-v1";
    private static final String NULL_VALUE = "<null>";

    public String build(ReqCreateNotificationDTO request) {
        StringBuilder canonical = new StringBuilder(FINGERPRINT_VERSION);
        appendCanonicalField(canonical, request.getTitle());
        appendCanonicalField(canonical, request.getBody());
        appendCanonicalField(canonical, enumValue(request.getAudienceType()));
        appendCanonicalField(canonical, canonicalTargetReference(request));
        appendCanonicalField(canonical, canonicalRecipientIds(request.getRecipientUserIds()));
        appendCanonicalField(canonical, enumValue(effectiveChannel(request.getChannel())));
        appendCanonicalField(canonical, stringValue(request.getPublishAt()));
        appendCanonicalField(canonical, stringValue(request.getExpiresAt()));
        appendCanonicalField(canonical, request.getSchoolScope());
        return sha256(canonical.toString());
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

    private String canonicalTargetReference(ReqCreateNotificationDTO request) {
        if (request.getAudienceType() == NotificationAudienceType.CLASS
                && request.getTargetReference() != null) {
            return request.getTargetReference().trim();
        }
        return request.getTargetReference();
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
}
