package com.JavaTraining.BaiTap_RS.notification.service;

import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqCreateNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqPublishNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationReceiptDTO;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationReceiptRepository;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationDraftService draftService;
    private final NotificationLifecycleService lifecycleService;
    private final NotificationQueryService queryService;
    private final NotificationReadService readService;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationReceiptRepository notificationReceiptRepository,
            NotificationAudienceService notificationAudienceService,
            NotificationAuditService notificationAuditService) {
        NotificationResponseMapper responseMapper = new NotificationResponseMapper();
        NotificationRequestValidator requestValidator = new NotificationRequestValidator();
        NotificationIdempotencyService idempotencyService =
                new NotificationIdempotencyService(notificationRepository);
        this.draftService = new NotificationDraftService(
                notificationAuditService,
                requestValidator,
                idempotencyService,
                responseMapper);
        this.lifecycleService = new NotificationLifecycleService(
                notificationRepository,
                notificationReceiptRepository,
                notificationAudienceService,
                notificationAuditService,
                responseMapper,
                requestValidator);
        this.queryService = new NotificationQueryService(
                notificationRepository,
                notificationReceiptRepository,
                responseMapper);
        this.readService = new NotificationReadService(
                notificationReceiptRepository,
                notificationRepository,
                notificationAuditService);
    }

    @Transactional
    public ResNotificationDTO createDraft(ReqCreateNotificationDTO request, Long actorUserId) {
        return draftService.createDraft(request, actorUserId);
    }

    @Transactional
    public ResNotificationDTO publish(Long id, ReqPublishNotificationDTO request, Long actorUserId) {
        return lifecycleService.publish(id, request, actorUserId);
    }

    @Transactional
    public ResNotificationDTO cancel(Long id, Long actorUserId) {
        return lifecycleService.cancel(id, actorUserId);
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO<ResNotificationDTO> getInbox(
            Long actorUserId, Boolean unreadOnly, Pageable pageable) {
        return queryService.getInbox(actorUserId, unreadOnly, pageable);
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO<ResNotificationDTO> getManagedNotifications(Long actorUserId, Pageable pageable) {
        return queryService.getManagedNotifications(pageable);
    }

    @Transactional(readOnly = true)
    public ResNotificationDTO getNotificationDetail(Long id, Long actorUserId, boolean isManager) {
        return queryService.getNotificationDetail(id, actorUserId, isManager);
    }

    @Transactional
    public ResNotificationReceiptDTO markAsRead(Long id, Long actorUserId) {
        return readService.markAsRead(id, actorUserId);
    }
}
