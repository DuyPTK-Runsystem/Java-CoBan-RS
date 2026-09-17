package com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationDeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResNotificationReceiptDTO {

    private Long id;
    private Long notificationId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long recipientUserId;
    private String accessScope;
    private LocalDateTime readAt;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private NotificationDeliveryStatus deliveryStatus;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String deliveryError;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime deliveredAt;
    private LocalDateTime createdAt;
}
