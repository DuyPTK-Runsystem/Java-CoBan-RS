package com.JavaTraining.BaiTap_RS.notification.service;

import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationStatus;

public class NotificationResponseMapper {

    public ResNotificationDTO toResponse(
            Notification notification,
            Boolean read,
            LocalDateTime readAt,
            boolean includeTargetingDetails) {
        NotificationStatus effectiveStatus = effectiveStatus(notification);
        return ResNotificationDTO.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .body(notification.getBody())
                .channel(notification.getChannel())
                .status(effectiveStatus)
                .audienceType(notification.getAudienceType())
                // targetReference contains internal class/user ids and must never cross the API boundary.
                .targetReference(null)
                .schoolScope(notification.getSchoolScope())
                .senderId(includeTargetingDetails ? notification.getSenderId() : null)
                .publishAt(notification.getPublishAt())
                .expiresAt(notification.getExpiresAt())
                .idempotencyKey(includeTargetingDetails ? notification.getIdempotencyKey() : null)
                .version(notification.getVersion())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .read(read)
                .readAt(readAt)
                .build();
    }

    private NotificationStatus effectiveStatus(Notification notification) {
        NotificationStatus status = notification.getStatus();
        if (status == NotificationStatus.PUBLISHED
                && notification.getExpiresAt() != null
                && !notification.getExpiresAt().isAfter(LocalDateTime.now())) {
            return NotificationStatus.EXPIRED;
        }
        return status;
    }
}
