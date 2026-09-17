package com.JavaTraining.BaiTap_RS.notification;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationAudienceType;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationReceipt;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationStatus;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationIndividualAudienceProjectionRepository;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationRepository;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationReceiptRepository;
import com.JavaTraining.BaiTap_RS.notification.service.NotificationAudienceDetailService;
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

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private NotificationIndividualAudienceProjectionRepository individualAudienceProjectionRepository;

    private NotificationQueryService managerAwareQueryService() {
        return new NotificationQueryService(
                notificationRepository,
                notificationReceiptRepository,
                new NotificationResponseMapper(),
                new NotificationAudienceDetailService(
                        notificationReceiptRepository,
                        schoolClassRepository,
                        individualAudienceProjectionRepository));
    }

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
    void managedNotificationsMapsLegacyRowsWithoutEmbeddedIdempotency() {
        Pageable pageable = PageRequest.of(0, 20);
        Notification legacyNotification = new Notification(
                "Thông báo cũ",
                "Dữ liệu trước embedded idempotency",
                NotificationAudienceType.SCHOOL,
                null,
                1L,
                "DEFAULT_SCHOOL");
        ReflectionTestUtils.setField(legacyNotification, "id", 7L);
        legacyNotification.setIdempotency(null);
        Mockito.when(notificationRepository.findBySchoolScope("DEFAULT_SCHOOL", pageable))
                .thenReturn(new PageImpl<>(List.of(legacyNotification), pageable, 1));

        NotificationQueryService queryService = new NotificationQueryService(
                notificationRepository,
                notificationReceiptRepository,
                new NotificationResponseMapper());

        ResultPaginationDTO<ResNotificationDTO> result = queryService.getManagedNotifications(pageable);

        Assertions.assertEquals(1, result.result().size());
        Assertions.assertEquals(7L, result.result().get(0).getId());
        Assertions.assertNull(result.result().get(0).getIdempotencyKey());
    }

    @Test
    void teacherManagedNotificationsAreRestrictedToTheirOwnSenderId() {
        Pageable pageable = PageRequest.of(0, 20);
        Notification notification = new Notification(
                "Thông báo của giáo viên",
                "Nội dung",
                NotificationAudienceType.SCHOOL,
                null,
                5L,
                "DEFAULT_SCHOOL");
        ReflectionTestUtils.setField(notification, "id", 8L);
        Mockito.when(notificationRepository.findBySenderId(5L, pageable))
                .thenReturn(new PageImpl<>(List.of(notification), pageable, 1));

        ResultPaginationDTO<ResNotificationDTO> result = managerAwareQueryService()
                .getManagedNotifications(5L, pageable, false);

        Assertions.assertEquals(1, result.result().size());
        Mockito.verify(notificationRepository).findBySenderId(5L, pageable);
        Mockito.verify(notificationRepository, Mockito.never()).findBySchoolScope(Mockito.anyString(), Mockito.any());
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

    @Test
    void managerDetailMapsClassAudienceDetailsWithoutRawTargetReference() {
        Notification notification = new Notification(
                "Thông báo lớp",
                "Nội dung lớp",
                NotificationAudienceType.CLASS,
                "101",
                1L,
                "DEFAULT_SCHOOL");
        ReflectionTestUtils.setField(notification, "id", 101L);
        Mockito.when(notificationRepository.findById(101L)).thenReturn(Optional.of(notification));
        SchoolClass schoolClass = Mockito.mock(SchoolClass.class);
        Mockito.when(schoolClass.getClassName()).thenReturn("Lớp 10A1");
        Mockito.when(schoolClassRepository.findById(101L)).thenReturn(Optional.of(schoolClass));
        Mockito.when(notificationReceiptRepository.findByNotificationId(101L)).thenReturn(List.of(
                new NotificationReceipt(101L, 9L, "CLASS"),
                new NotificationReceipt(101L, 10L, "CLASS")));

        ResNotificationDTO response = managerAwareQueryService().getNotificationDetail(101L, 1L, true);

        Assertions.assertNull(response.getTargetReference());
        Assertions.assertNotNull(response.getAudienceDetails());
        Assertions.assertEquals(NotificationAudienceType.CLASS, response.getAudienceDetails().audienceType());
        Assertions.assertEquals("Lớp 10A1", response.getAudienceDetails().displayLabel());
        Assertions.assertEquals(2, response.getAudienceDetails().recipientCount());
        Assertions.assertTrue(response.getAudienceDetails().displayDataAvailable());
    }

    @Test
    void managerDetailMapsIndividualAudienceDetailsWithoutRawTargetReference() {
        Notification notification = new Notification(
                "Thông báo cá nhân",
                "Nội dung cá nhân",
                NotificationAudienceType.INDIVIDUAL,
                "9,10",
                1L,
                "DEFAULT_SCHOOL");
        ReflectionTestUtils.setField(notification, "id", 102L);
        Mockito.when(notificationRepository.findById(102L)).thenReturn(Optional.of(notification));
        Mockito.when(notificationReceiptRepository.findByNotificationId(102L)).thenReturn(List.of(
                new NotificationReceipt(102L, 9L, "INDIVIDUAL"),
                new NotificationReceipt(102L, 10L, "INDIVIDUAL")));
        NotificationIndividualAudienceProjectionRepository.IndividualDisplayNameProjection first =
                Mockito.mock(NotificationIndividualAudienceProjectionRepository.IndividualDisplayNameProjection.class);
        NotificationIndividualAudienceProjectionRepository.IndividualDisplayNameProjection second =
                Mockito.mock(NotificationIndividualAudienceProjectionRepository.IndividualDisplayNameProjection.class);
        Mockito.when(first.getDisplayName()).thenReturn("Nguyễn Văn A");
        Mockito.when(second.getDisplayName()).thenReturn("Trần Thị B");
        Mockito.when(individualAudienceProjectionRepository.findDisplayNamesByUserIds(Mockito.any()))
                .thenReturn(List.of(first, second));

        ResNotificationDTO response = managerAwareQueryService().getNotificationDetail(102L, 1L, true);

        Assertions.assertNull(response.getTargetReference());
        Assertions.assertNotNull(response.getAudienceDetails());
        Assertions.assertEquals(NotificationAudienceType.INDIVIDUAL, response.getAudienceDetails().audienceType());
        Assertions.assertEquals("Người nhận cụ thể", response.getAudienceDetails().displayLabel());
        Assertions.assertEquals(2, response.getAudienceDetails().recipientCount());
        Assertions.assertEquals(List.of("Nguyễn Văn A", "Trần Thị B"),
                response.getAudienceDetails().recipientDisplayNames());
        Assertions.assertTrue(response.getAudienceDetails().displayDataAvailable());
    }

    @Test
    void recipientDetailHidesAudienceDetailsAndUnauthorizedActorIsForbidden() {
        Notification notification = new Notification(
                "Thông báo riêng tư",
                "Nội dung",
                NotificationAudienceType.INDIVIDUAL,
                "9,10",
                1L,
                "DEFAULT_SCHOOL");
        notification.setStatus(NotificationStatus.PUBLISHED);
        ReflectionTestUtils.setField(notification, "id", 103L);
        Mockito.when(notificationRepository.findById(103L)).thenReturn(Optional.of(notification));
        Mockito.when(notificationReceiptRepository.findByNotificationIdAndRecipientUserId(103L, 9L))
                .thenReturn(Optional.of(new NotificationReceipt(103L, 9L, "INDIVIDUAL")));
        Mockito.when(notificationReceiptRepository.findByNotificationIdAndRecipientUserId(103L, 999L))
                .thenReturn(Optional.empty());
        NotificationQueryService queryService = managerAwareQueryService();

        ResNotificationDTO recipientResponse = queryService.getNotificationDetail(103L, 9L, false);

        Assertions.assertNull(recipientResponse.getTargetReference());
        Assertions.assertNull(recipientResponse.getAudienceDetails());
        Mockito.verify(notificationReceiptRepository, Mockito.never()).findByNotificationId(103L);

        AppException exception = Assertions.assertThrows(
                AppException.class, () -> queryService.getNotificationDetail(103L, 999L, false));

        Assertions.assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        Mockito.verifyNoInteractions(schoolClassRepository, individualAudienceProjectionRepository);
    }
}
