package com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response;

import java.util.List;

import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationAudienceType;

/**
 * Manager-only, display-safe audience metadata. Internal ids are deliberately
 * not part of this contract.
 */
public record ResNotificationAudienceDetailsDTO(
        NotificationAudienceType audienceType,
        String displayLabel,
        Integer recipientCount,
        List<String> recipientDisplayNames,
        boolean displayDataAvailable) {
}
