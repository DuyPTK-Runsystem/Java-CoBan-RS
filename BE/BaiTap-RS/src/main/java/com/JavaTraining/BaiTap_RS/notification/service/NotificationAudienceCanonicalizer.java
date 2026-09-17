package com.JavaTraining.BaiTap_RS.notification.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import org.springframework.http.HttpStatus;

public class NotificationAudienceCanonicalizer {

    public List<Long> recipientIds(Collection<Long> recipientUserIds) {
        if (recipientUserIds == null) {
            return List.of();
        }
        Set<Long> canonicalIds = new TreeSet<>();
        for (Long userId : recipientUserIds) {
            if (userId == null || userId <= 0) {
                throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "Danh sách người nhận chứa user ID không hợp lệ");
            }
            canonicalIds.add(userId);
        }
        return new ArrayList<>(canonicalIds);
    }

    public String classReference(String targetReference) {
        return String.valueOf(parseClassId(targetReference));
    }

    public Set<Long> legacyIndividualReference(String targetReference) {
        Set<Long> userIds = new TreeSet<>();
        if (targetReference == null || targetReference.isBlank()) {
            return userIds;
        }
        for (String part : targetReference.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                userIds.add(parseUserId(trimmed));
            }
        }
        return userIds;
    }

    public Long parseClassId(String targetReference) {
        if (targetReference == null || targetReference.isBlank()) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "Mã lớp không được để trống");
        }
        try {
            return Long.valueOf(targetReference.trim());
        } catch (NumberFormatException exception) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "Mã lớp không hợp lệ: " + targetReference, exception);
        }
    }

    private Long parseUserId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new AppException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Mã người nhận không hợp lệ: " + value,
                    exception);
        }
    }
}
