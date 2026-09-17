package com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response;

public record ResNotificationClassAudienceDTO(
        Long classId,
        String classCode,
        String className,
        Long academicYearId,
        Long eligibleRecipientCount) {
}
