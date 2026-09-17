package com.JavaTraining.BaiTap_RS.notification.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationReceipt;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationStatus;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationReceiptRepository;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

public class NotificationQueryService {

    private static final String NOT_FOUND_PREFIX = "Không tìm thấy thông báo với ID: ";
    private static final String DEFAULT_SCHOOL_SCOPE = "DEFAULT_SCHOOL";

    private final NotificationRepository notificationRepository;
    private final NotificationReceiptRepository notificationReceiptRepository;
    private final NotificationResponseMapper responseMapper;
    private final NotificationAudienceDetailService audienceDetailService;

    public NotificationQueryService(
            NotificationRepository notificationRepository,
            NotificationReceiptRepository notificationReceiptRepository,
            NotificationResponseMapper responseMapper) {
        this(notificationRepository, notificationReceiptRepository, responseMapper, null);
    }

    public NotificationQueryService(
            NotificationRepository notificationRepository,
            NotificationReceiptRepository notificationReceiptRepository,
            NotificationResponseMapper responseMapper,
            NotificationAudienceDetailService audienceDetailService) {
        this.notificationRepository = notificationRepository;
        this.notificationReceiptRepository = notificationReceiptRepository;
        this.responseMapper = responseMapper;
        this.audienceDetailService = audienceDetailService;
    }

    public ResultPaginationDTO<ResNotificationDTO> getInbox(
            Long actorUserId, Boolean unreadOnly, Pageable pageable) {
        Page<NotificationReceipt> receiptsPage = unreadOnlyReceipts(actorUserId, unreadOnly, pageable);
        List<Long> notificationIds = receiptsPage.getContent().stream()
                .map(NotificationReceipt::getNotificationId)
                .toList();
        Map<Long, Notification> notifications = notificationRepository.findAllById(notificationIds).stream()
                .collect(Collectors.toMap(Notification::getId, notification -> notification));

        List<ResNotificationDTO> results = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (NotificationReceipt receipt : receiptsPage.getContent()) {
            Notification notification = notifications.get(receipt.getNotificationId());
            if (notification != null && isVisibleInInbox(notification, now)) {
                results.add(responseMapper.toResponse(
                        notification,
                        receipt.getReadAt() != null,
                        receipt.getReadAt(),
                        false));
            }
        }
        return new ResultPaginationDTO<>(toMeta(receiptsPage), results);
    }

    public ResultPaginationDTO<ResNotificationDTO> getManagedNotifications(Pageable pageable) {
        return getManagedNotifications(null, pageable, true);
    }

    public ResultPaginationDTO<ResNotificationDTO> getManagedNotifications(
            Long actorUserId, Pageable pageable, boolean canManageAll) {
        Page<Notification> page = canManageAll
                ? notificationRepository.findBySchoolScope(DEFAULT_SCHOOL_SCOPE, pageable)
                : notificationRepository.findBySenderId(actorUserId, pageable);
        List<ResNotificationDTO> results = page.getContent().stream()
                .map(notification -> responseMapper.toResponse(notification, null, null, true))
                .toList();
        return new ResultPaginationDTO<>(toMeta(page), results);
    }

    public ResNotificationDTO getNotificationDetail(Long id, Long actorUserId, boolean isManager) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, NOT_FOUND_PREFIX + id));
        if (!DEFAULT_SCHOOL_SCOPE.equals(notification.getSchoolScope())) {
            throw new AppException(HttpStatus.NOT_FOUND, NOT_FOUND_PREFIX + id);
        }
        Optional<NotificationReceipt> receiptOpt = notificationReceiptRepository
                .findByNotificationIdAndRecipientUserId(id, actorUserId);
        boolean canManage = isManager || Objects.equals(notification.getSenderId(), actorUserId);
        validateDetailAccess(notification, actorUserId, canManage, receiptOpt, id);

        LocalDateTime readAt = receiptOpt.map(NotificationReceipt::getReadAt).orElse(null);
        Boolean read = receiptOpt.map(receipt -> receipt.getReadAt() != null).orElse(null);
        ResNotificationDTO response = responseMapper.toResponse(notification, read, readAt, canManage);
        if (canManage && audienceDetailService != null) {
            response.setAudienceDetails(audienceDetailService.resolve(notification));
        }
        return response;
    }

    private void validateDetailAccess(
            Notification notification,
            Long actorUserId,
            boolean isManager,
            Optional<NotificationReceipt> receiptOpt,
            Long notificationId) {
        if (isManager || notification.getSenderId().equals(actorUserId)) {
            return;
        }
        if (receiptOpt.isEmpty()) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập thông báo này");
        }
        if (!isVisibleInInbox(notification, LocalDateTime.now())) {
            throw new AppException(HttpStatus.NOT_FOUND, NOT_FOUND_PREFIX + notificationId);
        }
    }

    private Page<NotificationReceipt> unreadOnlyReceipts(
            Long actorUserId, Boolean unreadOnly, Pageable pageable) {
        if (Boolean.TRUE.equals(unreadOnly)) {
            return notificationReceiptRepository
                    .findVisibleUnreadByRecipientUserIdOrderByCreatedAtDesc(actorUserId, pageable);
        }
        return notificationReceiptRepository
                .findVisibleByRecipientUserIdOrderByCreatedAtDesc(actorUserId, pageable);
    }

    private boolean isVisibleInInbox(Notification notification, LocalDateTime now) {
        return notification.getStatus() == NotificationStatus.PUBLISHED
                && (notification.getPublishAt() == null || !notification.getPublishAt().isAfter(now))
                && (notification.getExpiresAt() == null || notification.getExpiresAt().isAfter(now));
    }

    private ResultPaginationDTO.Meta toMeta(Page<?> page) {
        return new ResultPaginationDTO.Meta(
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements());
    }
}
