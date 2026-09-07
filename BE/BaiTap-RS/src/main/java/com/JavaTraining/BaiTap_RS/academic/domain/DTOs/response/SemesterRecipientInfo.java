package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response;

import java.util.List;

public record SemesterRecipientInfo(
        String recipientEmail,
        String recipientRole,
        Long recipientTeacherId,
        String recipientName,
        String subject,
        String bodyContent,
        List<String> scopedDetails) {
}
