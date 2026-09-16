package com.JavaTraining.BaiTap_RS.notification.service;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqCreateNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationChannel;
import org.springframework.http.HttpStatus;

public class NotificationRequestValidator {

    private static final String DEFAULT_SCHOOL_SCOPE = "DEFAULT_SCHOOL";

    private final NotificationAudienceValidator audienceValidator;

    public NotificationRequestValidator() {
        this(new NotificationAudienceValidator());
    }

    public NotificationRequestValidator(NotificationAudienceValidator audienceValidator) {
        this.audienceValidator = audienceValidator;
    }

    public void validateCreateRequest(ReqCreateNotificationDTO request) {
        if (request == null) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "Payload thông báo không được để trống");
        }
        if (request.getPublishAt() != null) {
            throw new AppException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Notification v3 hiện chỉ hỗ trợ publish ngay, không hỗ trợ lên lịch");
        }
        if (!request.hasValidExpiryWindow()) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "Thời điểm hết hạn phải sau thời điểm phát hành");
        }
        validateSchoolScope(request.getSchoolScope());
        validateChannel(request.getChannel());
        audienceValidator.validateCreate(request);
    }

    public void validateStoredNotification(Notification notification) {
        if (notification == null) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "Thông báo không được để trống");
        }
        validateSchoolScope(notification.getSchoolScope());
        validateChannel(notification.getChannel());
        audienceValidator.validateStored(notification);
    }

    public void validateChannel(NotificationChannel channel) {
        if (channel != null && channel != NotificationChannel.IN_APP) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "Notification v3 chỉ hỗ trợ kênh IN_APP");
        }
    }

    private void validateSchoolScope(String schoolScope) {
        if (!DEFAULT_SCHOOL_SCOPE.equals(schoolScope)) {
            throw new AppException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Notification v3 chỉ hỗ trợ school scope DEFAULT_SCHOOL");
        }
    }

}
