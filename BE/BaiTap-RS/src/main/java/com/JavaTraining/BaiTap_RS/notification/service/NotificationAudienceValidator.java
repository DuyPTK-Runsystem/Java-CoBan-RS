package com.JavaTraining.BaiTap_RS.notification.service;

import java.util.Arrays;
import java.util.List;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqCreateNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationAudienceType;
import org.springframework.http.HttpStatus;

public class NotificationAudienceValidator {

    public void validateCreate(ReqCreateNotificationDTO request) {
        if (request.getAudienceType() == null) {
            throw new AppException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Loại đối tượng nhận thông báo không được để trống");
        }

        switch (request.getAudienceType()) {
            case SCHOOL -> {
                rejectTargetReference(request.getTargetReference(), "SCHOOL");
                rejectRecipientIds(request.getRecipientUserIds(), "SCHOOL");
            }
            case CLASS -> {
                validateClassReference(request.getTargetReference());
                rejectRecipientIds(request.getRecipientUserIds(), "CLASS");
            }
            case INDIVIDUAL -> {
                rejectTargetReference(request.getTargetReference(), "INDIVIDUAL");
                validateRecipientIds(request.getRecipientUserIds());
            }
        }
    }

    public void validateStored(Notification notification) {
        NotificationAudienceType audienceType = notification.getAudienceType();
        if (audienceType == null) {
            throw new AppException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Loại đối tượng nhận thông báo không được để trống");
        }

        switch (audienceType) {
            case SCHOOL -> rejectTargetReference(notification.getTargetReference(), "SCHOOL");
            case CLASS -> validateClassReference(notification.getTargetReference());
            case INDIVIDUAL -> validateIndividualTargetReference(notification.getTargetReference());
        }
    }

    private void validateClassReference(String targetReference) {
        if (targetReference == null || targetReference.isBlank()) {
            throw new AppException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Mã lớp không được để trống đối với thông báo theo lớp");
        }
        if (!isPositiveLong(targetReference.trim())) {
            throw new AppException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Mã lớp phải là một ID dương hợp lệ: " + targetReference);
        }
    }

    private void validateRecipientIds(List<Long> recipientUserIds) {
        if (recipientUserIds == null || recipientUserIds.isEmpty()) {
            throw new AppException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Danh sách người nhận cho thông báo cá nhân không được để trống");
        }
        if (recipientUserIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new AppException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Danh sách người nhận chứa user ID không hợp lệ");
        }
    }

    private void validateIndividualTargetReference(String targetReference) {
        if (targetReference == null || targetReference.isBlank()) {
            throw new AppException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Danh sách người nhận cho thông báo cá nhân không được để trống");
        }
        String[] parts = targetReference.split(",");
        if (Arrays.stream(parts).anyMatch(part -> !isPositiveLong(part.trim()))) {
            throw new AppException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Danh sách người nhận chứa user ID không hợp lệ");
        }
    }

    private boolean isPositiveLong(String value) {
        try {
            return !value.isBlank() && Long.parseLong(value) > 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private void rejectTargetReference(String targetReference, String audienceType) {
        if (targetReference != null && !targetReference.isBlank()) {
            throw new AppException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Audience " + audienceType + " không được nhận targetReference");
        }
    }

    private void rejectRecipientIds(List<Long> recipientUserIds, String audienceType) {
        if (recipientUserIds != null && !recipientUserIds.isEmpty()) {
            throw new AppException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Audience " + audienceType + " không được nhận recipientUserIds");
        }
    }
}
