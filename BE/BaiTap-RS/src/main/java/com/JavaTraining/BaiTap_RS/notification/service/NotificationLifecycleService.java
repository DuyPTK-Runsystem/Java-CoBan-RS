package com.JavaTraining.BaiTap_RS.notification.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqPublishNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationReceipt;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationStatus;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationReceiptRepository;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationRepository;
import org.springframework.http.HttpStatus;

public class NotificationLifecycleService {

    private static final String NOT_FOUND_PREFIX = "Không tìm thấy thông báo với ID: ";

    private final NotificationRepository notificationRepository;
    private final NotificationReceiptRepository notificationReceiptRepository;
    private final NotificationAudienceService notificationAudienceService;
    private final NotificationAuditService notificationAuditService;
    private final NotificationResponseMapper responseMapper;
    private final NotificationRequestValidator requestValidator;

    public NotificationLifecycleService(
            NotificationRepository notificationRepository,
            NotificationReceiptRepository notificationReceiptRepository,
            NotificationAudienceService notificationAudienceService,
            NotificationAuditService notificationAuditService,
            NotificationResponseMapper responseMapper,
            NotificationRequestValidator requestValidator) {
        this.notificationRepository = notificationRepository;
        this.notificationReceiptRepository = notificationReceiptRepository;
        this.notificationAudienceService = notificationAudienceService;
        this.notificationAuditService = notificationAuditService;
        this.responseMapper = responseMapper;
        this.requestValidator = requestValidator;
    }

    public ResNotificationDTO publish(Long id, ReqPublishNotificationDTO request, Long actorUserId) {
        Notification notification = findNotification(id);
        if (notification.getStatus() == NotificationStatus.PUBLISHED) {
            return responseMapper.toResponse(notification, null, null, true);
        }
        validatePublishState(notification, request);
        validateExpiryBeforePublish(notification);
        requestValidator.validateStoredNotification(notification);

        Set<Long> recipientUserIds = notificationAudienceService.resolveAudienceUserIds(notification, null);
        createReceiptsForRecipients(id, recipientUserIds, notification.getAudienceType().name());
        applyPublishStatus(notification);

        Notification saved = notificationRepository.save(notification);
        notificationAuditService.auditPublish(actorUserId, saved, recipientUserIds.size());
        return responseMapper.toResponse(saved, null, null, true);
    }

    public ResNotificationDTO cancel(Long id, Long actorUserId) {
        Notification notification = findNotification(id);
        if (notification.getStatus() == NotificationStatus.CANCELLED) {
            return responseMapper.toResponse(notification, null, null, true);
        }
        if (notification.getStatus() == NotificationStatus.PUBLISHED) {
            throw new AppException(HttpStatus.CONFLICT, "Không thể hủy thông báo đã xuất bản");
        }
        if (notification.getStatus() == NotificationStatus.EXPIRED) {
            throw new AppException(HttpStatus.CONFLICT, "Không thể hủy thông báo đã hết hạn");
        }

        notification.setStatus(NotificationStatus.CANCELLED);
        Notification saved = notificationRepository.save(notification);
        notificationAuditService.auditCancel(actorUserId, saved);
        return responseMapper.toResponse(saved, null, null, true);
    }

    private Notification findNotification(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, NOT_FOUND_PREFIX + id));
    }

    private void validatePublishState(Notification notification, ReqPublishNotificationDTO request) {
        if (notification.getStatus() == NotificationStatus.CANCELLED) {
            throw new AppException(HttpStatus.CONFLICT, "Không thể xuất bản thông báo đã bị hủy");
        }
        if (notification.getStatus() == NotificationStatus.EXPIRED) {
            throw new AppException(HttpStatus.CONFLICT, "Không thể xuất bản thông báo đã hết hạn");
        }
        if (request != null && request.getExpectedVersion() != null
                && !request.getExpectedVersion().equals(notification.getVersion())) {
            throw new AppException(HttpStatus.CONFLICT, "Dữ liệu thông báo đã thay đổi, vui lòng tải lại");
        }
    }

    private void createReceiptsForRecipients(Long notificationId, Set<Long> recipientUserIds, String scope) {
        List<NotificationReceipt> receiptsToSave = new ArrayList<>();
        for (Long recipientUserId : recipientUserIds) {
            if (!notificationReceiptRepository.existsByNotificationIdAndRecipientUserId(
                    notificationId, recipientUserId)) {
                receiptsToSave.add(new NotificationReceipt(notificationId, recipientUserId, scope));
            }
        }
        if (!receiptsToSave.isEmpty()) {
            notificationReceiptRepository.saveAll(receiptsToSave);
        }
    }

    private void applyPublishStatus(Notification notification) {
        notification.setStatus(NotificationStatus.PUBLISHED);
        notification.setPublishAt(LocalDateTime.now());
    }

    private void validateExpiryBeforePublish(Notification notification) {
        LocalDateTime now = LocalDateTime.now();
        if (notification.getPublishAt() != null
                && notification.getExpiresAt() != null
                && !notification.getExpiresAt().isAfter(notification.getPublishAt())) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "Thời điểm hết hạn phải sau thời điểm phát hành");
        }
        if (notification.getExpiresAt() != null && !notification.getExpiresAt().isAfter(now)) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "Không thể xuất bản thông báo đã hết hạn");
        }
    }
}
