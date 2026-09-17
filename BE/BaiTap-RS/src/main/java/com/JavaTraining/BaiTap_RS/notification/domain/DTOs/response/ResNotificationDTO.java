package com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response;

import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationAudienceType;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationChannel;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.TooManyFields")
public class ResNotificationDTO {

    private Long id;
    private String title;
    private String body;
    private NotificationChannel channel;
    private NotificationStatus status;
    private NotificationAudienceType audienceType;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String targetReference;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ResNotificationAudienceDetailsDTO audienceDetails;
    private String schoolScope;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long senderId;
    private LocalDateTime publishAt;
    private LocalDateTime expiresAt;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String idempotencyKey;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean read;
    private LocalDateTime readAt;
}
