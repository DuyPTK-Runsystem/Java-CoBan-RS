package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqCreateNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqPublishNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationAudienceType;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationChannel;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationRepository;
import com.JavaTraining.BaiTap_RS.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LibraryInAppNotificationService {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    public LibraryInAppNotificationService(NotificationService notificationService,
            NotificationRepository notificationRepository) {
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void publish(String eventKey, String title, String body, Long recipientUserId, Long actorUserId) {
        if (notificationRepository.findByIdempotencyKey(eventKey).isPresent()) {
            return;
        }
        if (actorUserId == null) {
            throw new LibraryCirculationException(HttpStatus.SERVICE_UNAVAILABLE,
                    "LIBRARY_NOTIFICATION_ACTOR_REQUIRED", "Không thể ghi nhận người khởi tạo thông báo thư viện");
        }
        ReqCreateNotificationDTO create = ReqCreateNotificationDTO.builder()
                .title(title)
                .body(body)
                .audienceType(NotificationAudienceType.INDIVIDUAL)
                .recipientUserIds(List.of(recipientUserId))
                .channel(NotificationChannel.IN_APP)
                .schoolScope("DEFAULT_SCHOOL")
                .idempotencyKey(eventKey)
                .build();
        ResNotificationDTO draft = notificationService.createDraft(create, actorUserId);
        ReqPublishNotificationDTO publish = ReqPublishNotificationDTO.builder()
                .expectedVersion(draft.getVersion())
                .idempotencyKey(eventKey)
                .build();
        notificationService.publish(draft.getId(), publish, actorUserId);
    }
}
