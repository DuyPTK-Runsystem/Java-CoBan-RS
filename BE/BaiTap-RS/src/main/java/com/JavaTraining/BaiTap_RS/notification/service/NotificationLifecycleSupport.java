package com.JavaTraining.BaiTap_RS.notification.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqPublishNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationReceipt;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationStatus;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationReceiptRepository;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationRepository;
import org.springframework.http.HttpStatus;

final class NotificationLifecycleSupport {
    private static final String NOT_FOUND_PREFIX = "Không tìm thấy thông báo với ID: ";
    private final NotificationRepository notificationRepository;
    private final NotificationReceiptRepository receiptRepository;

    public NotificationLifecycleSupport(NotificationRepository notificationRepository,
            NotificationReceiptRepository receiptRepository) {
        this.notificationRepository = notificationRepository;
        this.receiptRepository = receiptRepository;
    }

    public Notification find(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, NOT_FOUND_PREFIX + id));
    }

    public void validateAccess(Notification notification, Long actorUserId, boolean canManageAll) {
        if (!canManageAll && !Objects.equals(notification.getSenderId(), actorUserId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không có quyền quản lý thông báo này");
        }
    }

    public void validatePublishState(Notification notification, ReqPublishNotificationDTO request) {
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

    public void validateExpiry(Notification notification) {
        LocalDateTime now = LocalDateTime.now();
        if (notification.getPublishAt() != null && notification.getExpiresAt() != null
                && !notification.getExpiresAt().isAfter(notification.getPublishAt())) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Thời điểm hết hạn phải sau thời điểm phát hành");
        }
        if (notification.getExpiresAt() != null && !notification.getExpiresAt().isAfter(now)) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "Không thể xuất bản thông báo đã hết hạn");
        }
    }

    public List<NotificationReceipt> createReceipts(Long notificationId, Set<Long> recipientUserIds, String scope) {
        List<NotificationReceipt> receiptsToSave = new ArrayList<>();
        for (Long recipientUserId : recipientUserIds) {
            if (!receiptRepository.existsByNotificationIdAndRecipientUserId(notificationId, recipientUserId)) {
                receiptsToSave.add(new NotificationReceipt(notificationId, recipientUserId, scope));
            }
        }
        return receiptsToSave.isEmpty()
                ? receiptRepository.findByNotificationId(notificationId)
                : receiptRepository.saveAll(receiptsToSave);
    }

    public void markPublished(Notification notification) {
        notification.setStatus(NotificationStatus.PUBLISHED);
        notification.setPublishAt(LocalDateTime.now());
    }
}
