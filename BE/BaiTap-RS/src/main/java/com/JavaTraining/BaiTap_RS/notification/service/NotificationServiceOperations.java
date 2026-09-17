package com.JavaTraining.BaiTap_RS.notification.service;

import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqCreateNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqPublishNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationReceiptDTO;
import org.springframework.data.domain.Pageable;

public final class NotificationServiceOperations {
    private final NotificationDraftService draftService;
    private final NotificationLifecycleService lifecycleService;
    private final NotificationQueryService queryService;
    private final NotificationReadService readService;

    public NotificationServiceOperations(NotificationServiceWiring wiring) {
        this.draftService = wiring.draftService();
        this.lifecycleService = wiring.lifecycleService();
        this.queryService = wiring.queryService();
        this.readService = wiring.readService();
    }

    public ResNotificationDTO createDraft(ReqCreateNotificationDTO request, Long actorUserId) {
        return draftService.createDraft(request, actorUserId);
    }

    public ResNotificationDTO publish(Long id, ReqPublishNotificationDTO request, Long actorUserId,
            boolean canManageAll) {
        return lifecycleService.publish(id, request, actorUserId, canManageAll);
    }

    public ResNotificationDTO cancel(Long id, Long actorUserId, boolean canManageAll) {
        return lifecycleService.cancel(id, actorUserId, canManageAll);
    }

    public ResultPaginationDTO<ResNotificationDTO> getInbox(Long actorUserId, Boolean unreadOnly, Pageable pageable) {
        return queryService.getInbox(actorUserId, unreadOnly, pageable);
    }

    public ResultPaginationDTO<ResNotificationDTO> getManagedNotifications(Long actorUserId, Pageable pageable,
            boolean canManageAll) {
        return queryService.getManagedNotifications(actorUserId, pageable, canManageAll);
    }

    public ResNotificationDTO getNotificationDetail(Long id, Long actorUserId, boolean isManager) {
        return queryService.getNotificationDetail(id, actorUserId, isManager);
    }

    public ResNotificationReceiptDTO markAsRead(Long id, Long actorUserId) {
        return readService.markAsRead(id, actorUserId);
    }
}
