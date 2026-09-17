package com.JavaTraining.BaiTap_RS.notification.service;

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

    private final NotificationRepository notificationRepository;
    private final NotificationAudienceService notificationAudienceService;
    private final NotificationAuditService notificationAuditService;
    private final NotificationResponseMapper responseMapper;
    private final NotificationRequestValidator requestValidator;
    private final NotificationEmailDeliveryService emailDeliveryService;
    private final NotificationLifecycleSupport lifecycleSupport;

    public NotificationLifecycleService(
            NotificationRepository notificationRepository,
            NotificationReceiptRepository notificationReceiptRepository,
            NotificationAudienceService notificationAudienceService,
            NotificationAuditService notificationAuditService,
            NotificationResponseMapper responseMapper,
            NotificationRequestValidator requestValidator) {
        this(
                notificationRepository,
                notificationReceiptRepository,
                notificationAudienceService,
                notificationAuditService,
                responseMapper,
                requestValidator,
                null);
    }

    public NotificationLifecycleService(
            NotificationRepository notificationRepository,
            NotificationReceiptRepository notificationReceiptRepository,
            NotificationAudienceService notificationAudienceService,
            NotificationAuditService notificationAuditService,
            NotificationResponseMapper responseMapper,
            NotificationRequestValidator requestValidator,
            NotificationEmailDeliveryService emailDeliveryService) {
        this.notificationRepository = notificationRepository;
        this.notificationAudienceService = notificationAudienceService;
        this.notificationAuditService = notificationAuditService;
        this.responseMapper = responseMapper;
        this.requestValidator = requestValidator;
        this.emailDeliveryService = emailDeliveryService;
        this.lifecycleSupport = new NotificationLifecycleSupport(notificationRepository, notificationReceiptRepository);
    }

    public ResNotificationDTO publish(Long id, ReqPublishNotificationDTO request, Long actorUserId) {
        return publish(id, request, actorUserId, true);
    }

    public ResNotificationDTO publish(
            Long id, ReqPublishNotificationDTO request, Long actorUserId, boolean canManageAll) {
        Notification notification = lifecycleSupport.find(id);
        lifecycleSupport.validateAccess(notification, actorUserId, canManageAll);
        if (notification.getStatus() == NotificationStatus.PUBLISHED) {
            return responseMapper.toResponse(notification, null, null, true);
        }
        lifecycleSupport.validatePublishState(notification, request);
        lifecycleSupport.validateExpiry(notification);
        requestValidator.validateStoredNotification(notification);

        Set<Long> recipientUserIds = notificationAudienceService.resolveAudienceUserIds(notification, null);
        List<NotificationReceipt> receipts = lifecycleSupport.createReceipts(
                id, recipientUserIds, notification.getAudienceType().name());
        if (emailDeliveryService != null) {
            emailDeliveryService.deliverIfRequired(notification, receipts);
        }
        lifecycleSupport.markPublished(notification);

        Notification saved = notificationRepository.save(notification);
        notificationAuditService.auditPublish(actorUserId, saved, recipientUserIds.size());
        return responseMapper.toResponse(saved, null, null, true);
    }

    public ResNotificationDTO cancel(Long id, Long actorUserId) {
        return cancel(id, actorUserId, true);
    }

    public ResNotificationDTO cancel(Long id, Long actorUserId, boolean canManageAll) {
        Notification notification = lifecycleSupport.find(id);
        lifecycleSupport.validateAccess(notification, actorUserId, canManageAll);
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

}
