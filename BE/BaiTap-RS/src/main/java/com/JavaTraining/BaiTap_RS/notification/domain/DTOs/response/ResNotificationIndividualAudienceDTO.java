package com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response;

import java.util.List;

public record ResNotificationIndividualAudienceDTO(
        Long userId,
        String displayName,
        String studentCode,
        String username,
        List<String> roleCodes,
        ResNotificationAudienceClassContextDTO studentClass,
        List<ResNotificationAudienceClassContextDTO> teacherClasses) {

    public ResNotificationIndividualAudienceDTO(
            Long userId, String displayName, String studentCode, String username) {
        this(userId, displayName, studentCode, username, List.of(), null, List.of());
    }
}
