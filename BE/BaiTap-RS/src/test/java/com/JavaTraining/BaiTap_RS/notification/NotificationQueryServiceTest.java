package com.JavaTraining.BaiTap_RS.notification;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationAudienceType;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationStatus;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationReceiptRepository;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationRepository;
import com.JavaTraining.BaiTap_RS.notification.service.NotificationQueryService;
import com.JavaTraining.BaiTap_RS.notification.service.NotificationResponseMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationReceiptRepository notificationReceiptRepository;

    @Test
    void managedNotificationsQueryIsRestrictedToDefaultSchoolScope() {
        Pageable pageable = PageRequest.of(0, 20);
        Notification notification = new Notification(
                "Thông báo quản lý",
                "Nội dung",
                NotificationAudienceType.SCHOOL,
                null,
                1L,
                "DEFAULT_SCHOOL");
        ReflectionTestUtils.setField(notification, "id", 1L);
        Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);
        Mockito.when(notificationRepository.findBySchoolScope("DEFAULT_SCHOOL", pageable)).thenReturn(page);

        NotificationQueryService queryService = new NotificationQueryService(
                notificationRepository,
                notificationReceiptRepository,
                new NotificationResponseMapper());

        ResultPaginationDTO<ResNotificationDTO> result = queryService.getManagedNotifications(pageable);

        Assertions.assertEquals(1, result.result().size());
        Assertions.assertEquals("DEFAULT_SCHOOL", result.result().get(0).getSchoolScope());
        Mockito.verify(notificationRepository).findBySchoolScope("DEFAULT_SCHOOL", pageable);
        Mockito.verify(notificationRepository, Mockito.never()).findAll(pageable);
    }

    @Test
    void detailRejectsNotificationOutsideDefaultSchoolScope() {
        Notification notification = new Notification(
                "Ngoài scope",
                "Không được trả về",
                NotificationAudienceType.SCHOOL,
                null,
                1L,
                "OTHER_SCHOOL");
        notification.setStatus(NotificationStatus.PUBLISHED);
        ReflectionTestUtils.setField(notification, "id", 2L);
        Mockito.when(notificationRepository.findById(2L)).thenReturn(Optional.of(notification));

        NotificationQueryService queryService = new NotificationQueryService(
                notificationRepository,
                notificationReceiptRepository,
                new NotificationResponseMapper());

        AppException exception = Assertions.assertThrows(
                AppException.class, () -> queryService.getNotificationDetail(2L, 1L, true));

        Assertions.assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        Mockito.verifyNoInteractions(notificationReceiptRepository);
    }
}
