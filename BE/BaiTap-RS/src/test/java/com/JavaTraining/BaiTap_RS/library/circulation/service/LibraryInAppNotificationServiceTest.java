package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.util.Optional;

import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqCreateNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqPublishNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationChannel;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationRepository;
import com.JavaTraining.BaiTap_RS.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LibraryInAppNotificationServiceTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private LibraryInAppNotificationService service;

    @Test
    void retryWithTheSameReservationEventKeyDoesNotCreateAnotherNotification() {
        String eventKey = "library-reservation-ready-41";
        when(notificationRepository.findByIdempotencyKey(eventKey))
                .thenReturn(Optional.empty(), Optional.of(mock(Notification.class)));
        when(notificationService.createDraft(org.mockito.ArgumentMatchers.any(ReqCreateNotificationDTO.class),
                org.mockito.ArgumentMatchers.eq(8L)))
                .thenReturn(ResNotificationDTO.builder().id(99L).version(1L).build());

        service.publish(eventKey, "Ready", "Pickup available", 15L, 8L);
        service.publish(eventKey, "Ready", "Pickup available", 15L, 8L);

        ArgumentCaptor<ReqCreateNotificationDTO> request = ArgumentCaptor.forClass(ReqCreateNotificationDTO.class);
        verify(notificationService).createDraft(request.capture(), org.mockito.ArgumentMatchers.eq(8L));
        verify(notificationService).publish(org.mockito.ArgumentMatchers.eq(99L),
                org.mockito.ArgumentMatchers.any(ReqPublishNotificationDTO.class), org.mockito.ArgumentMatchers.eq(8L));
        assertEquals(eventKey, request.getValue().getIdempotencyKey());
        assertEquals(NotificationChannel.IN_APP, request.getValue().getChannel());
    }
}
