package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response;

import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.NotificationChannel;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.NotificationStatus;

public record ResSemesterNotificationDTO(
        Long id,
        Long semesterId,
        Long reportId,
        String checkpointCode,
        String recipientEmail,
        String recipientRole,
        Long recipientTeacherId,
        NotificationChannel notificationChannel,
        NotificationStatus status,
        String subject,
        String bodyContent,
        int attemptCount,
        LocalDateTime sentAt,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
