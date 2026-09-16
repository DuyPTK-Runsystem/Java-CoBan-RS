package com.JavaTraining.BaiTap_RS.notification.service;

import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationReceiptDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationReceipt;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationReceiptRepository;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationRepository;
import org.springframework.http.HttpStatus;

public class NotificationReadService {

    private static final String NOT_FOUND_PREFIX = "Không tìm thấy thông báo với ID: ";

    private final NotificationReceiptRepository notificationReceiptRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationAuditService notificationAuditService;

    public NotificationReadService(
            NotificationReceiptRepository notificationReceiptRepository,
            NotificationRepository notificationRepository,
            NotificationAuditService notificationAuditService) {
        this.notificationReceiptRepository = notificationReceiptRepository;
        this.notificationRepository = notificationRepository;
        this.notificationAuditService = notificationAuditService;
    }

    public ResNotificationReceiptDTO markAsRead(Long id, Long actorUserId) {
        NotificationReceipt receipt = notificationReceiptRepository
                .findByNotificationIdAndRecipientUserId(id, actorUserId)
                .orElse(null);
        if (receipt == null) {
            if (!notificationRepository.existsById(id)) {
                throw new AppException(HttpStatus.NOT_FOUND, NOT_FOUND_PREFIX + id);
            }
            throw new AppException(HttpStatus.FORBIDDEN, "Bạn không thuộc danh sách người nhận thông báo này");
        }
        if (receipt.getReadAt() == null) {
            receipt.setReadAt(LocalDateTime.now());
            receipt = notificationReceiptRepository.save(receipt);
            notificationAuditService.auditMarkRead(actorUserId, id);
        }
        return new ResNotificationReceiptDTO(
                receipt.getId(),
                receipt.getNotificationId(),
                null,
                receipt.getAccessScope(),
                receipt.getReadAt(),
                receipt.getCreatedAt());
    }
}
