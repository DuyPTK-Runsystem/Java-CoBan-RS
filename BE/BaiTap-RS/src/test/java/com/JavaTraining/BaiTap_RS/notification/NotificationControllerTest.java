package com.JavaTraining.BaiTap_RS.notification;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.notification.controller.NotificationController;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqCreateNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqPublishNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationReceiptDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationAudienceType;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationChannel;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationStatus;
import com.JavaTraining.BaiTap_RS.notification.service.NotificationService;
import com.JavaTraining.BaiTap_RS.security.UserPrincipal;
import com.JavaTraining.BaiTap_RS.user.domain.entity.Role;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
        "PMD.UnitTestAssertionsShouldIncludeMessage",
        "PMD.UnitTestContainsTooManyAsserts"
})
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    private NotificationController controller;
    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        controller = new NotificationController(notificationService);

        User adminUser = new User("admin", "password");
        ReflectionTestUtils.setField(adminUser, "id", 1L);
        Role adminRole = new Role("ADMIN", "Administrator", "Admin");
        adminUser.addRole(adminRole);
        adminPrincipal = new UserPrincipal(adminUser);
    }

    @Test
    void createDraftDelegatesToService() {
        ReqCreateNotificationDTO req = ReqCreateNotificationDTO.builder()
                .title("Thông báo")
                .body("Nội dung")
                .audienceType(NotificationAudienceType.SCHOOL)
                .build();

        ResNotificationDTO expected = ResNotificationDTO.builder()
                .id(1L)
                .title("Thông báo")
                .status(NotificationStatus.DRAFT)
                .build();

        Mockito.when(notificationService.createDraft(req, 1L)).thenReturn(expected);

        ResponseEntity<ResNotificationDTO> response = controller.create(req, adminPrincipal);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Assertions.assertEquals(expected, response.getBody());
        Mockito.verify(notificationService).createDraft(req, 1L);
    }

    @Test
    void inboxDelegatesToService() {
        Pageable pageable = PageRequest.of(0, 20);
        ResultPaginationDTO<ResNotificationDTO> expected = new ResultPaginationDTO<>(
                new ResultPaginationDTO.Meta(0, 20, 0, 0), List.of());

        Mockito.when(notificationService.getInbox(1L, false, pageable)).thenReturn(expected);

        ResultPaginationDTO<ResNotificationDTO> res = controller.inbox(false, 0, 20, adminPrincipal);

        Assertions.assertEquals(expected, res);
        Mockito.verify(notificationService).getInbox(1L, false, pageable);
    }

    @Test
    void manageDelegatesToService() {
        Pageable pageable = PageRequest.of(0, 20);
        ResultPaginationDTO<ResNotificationDTO> expected = new ResultPaginationDTO<>(
                new ResultPaginationDTO.Meta(0, 20, 0, 0), List.of());

        Mockito.when(notificationService.getManagedNotifications(1L, pageable, true)).thenReturn(expected);

        ResultPaginationDTO<ResNotificationDTO> res = controller.manage(0, 20, adminPrincipal);

        Assertions.assertEquals(expected, res);
        Mockito.verify(notificationService).getManagedNotifications(1L, pageable, true);
    }

    @Test
    void detailDelegatesToService() {
        ResNotificationDTO expected = ResNotificationDTO.builder().id(5L).build();
        Mockito.when(notificationService.getNotificationDetail(5L, 1L, true)).thenReturn(expected);

        ResNotificationDTO res = controller.detail(5L, adminPrincipal);

        Assertions.assertEquals(expected, res);
        Mockito.verify(notificationService).getNotificationDetail(5L, 1L, true);
    }

    @Test
    void publishDelegatesToService() {
        ReqPublishNotificationDTO req = ReqPublishNotificationDTO.builder().build();
        ResNotificationDTO expected = ResNotificationDTO.builder().id(5L).status(NotificationStatus.PUBLISHED).build();
        Mockito.when(notificationService.publish(5L, req, 1L, true)).thenReturn(expected);

        ResNotificationDTO res = controller.publish(5L, req, adminPrincipal);

        Assertions.assertEquals(expected, res);
        Mockito.verify(notificationService).publish(5L, req, 1L, true);
    }

    @Test
    void cancelDelegatesToService() {
        ResNotificationDTO expected = ResNotificationDTO.builder().id(5L).status(NotificationStatus.CANCELLED).build();
        Mockito.when(notificationService.cancel(5L, 1L, true)).thenReturn(expected);

        ResNotificationDTO res = controller.cancel(5L, adminPrincipal);

        Assertions.assertEquals(expected, res);
        Mockito.verify(notificationService).cancel(5L, 1L, true);
    }

    @Test
    void markReadDelegatesToService() {
        ResNotificationReceiptDTO expected = ResNotificationReceiptDTO.builder().id(10L).build();
        Mockito.when(notificationService.markAsRead(5L, 1L)).thenReturn(expected);

        ResNotificationReceiptDTO res = controller.markRead(5L, adminPrincipal);

        Assertions.assertEquals(expected, res);
        Mockito.verify(notificationService).markAsRead(5L, 1L);
    }

    @Test
    void createDirectUnitPropagatesBadRequestStatus() {
        ReqCreateNotificationDTO req = ReqCreateNotificationDTO.builder().build();
        Mockito.when(notificationService.createDraft(req, 1L))
                .thenThrow(new AppException(HttpStatus.BAD_REQUEST, "payload không hợp lệ"));

        AppException ex = Assertions.assertThrows(AppException.class, () -> controller.create(req, adminPrincipal));

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void detailDirectUnitPropagatesForbiddenStatus() {
        Mockito.when(notificationService.getNotificationDetail(9L, 1L, true))
                .thenThrow(new AppException(HttpStatus.FORBIDDEN, "không có quyền"));

        AppException ex = Assertions.assertThrows(
                AppException.class, () -> controller.detail(9L, adminPrincipal));

        Assertions.assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void detailDirectUnitPropagatesNotFoundStatus() {
        Mockito.when(notificationService.getNotificationDetail(9L, 1L, true))
                .thenThrow(new AppException(HttpStatus.NOT_FOUND, "không tìm thấy"));

        AppException ex = Assertions.assertThrows(
                AppException.class, () -> controller.detail(9L, adminPrincipal));

        Assertions.assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void publishDirectUnitPropagatesConflictStatus() {
        ReqPublishNotificationDTO req = ReqPublishNotificationDTO.builder().build();
        Mockito.when(notificationService.publish(9L, req, 1L, true))
                .thenThrow(new AppException(HttpStatus.CONFLICT, "version conflict"));

        AppException ex = Assertions.assertThrows(
                AppException.class, () -> controller.publish(9L, req, adminPrincipal));

        Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void securedInboxDeclaresAuthenticationButDirectUnitDoesNotProve401() throws NoSuchMethodException {
        PreAuthorize preAuthorize = NotificationController.class
                .getMethod("inbox", Boolean.class, int.class, int.class, UserPrincipal.class)
                .getAnnotation(PreAuthorize.class);

        Assertions.assertNotNull(preAuthorize);
        Assertions.assertEquals("isAuthenticated()", preAuthorize.value());
    }

    @Test
    void zeroBasedPageOneIsPassedToNotificationServiceWithoutGlobalPageableResolver() {
        ResultPaginationDTO<ResNotificationDTO> expected = new ResultPaginationDTO<>(
                new ResultPaginationDTO.Meta(1, 20, 2, 21), List.of());
        Pageable expectedPageable = PageRequest.of(1, 20);
        Mockito.when(notificationService.getInbox(1L, false, expectedPageable)).thenReturn(expected);

        ResultPaginationDTO<ResNotificationDTO> response = controller.inbox(false, 1, 20, adminPrincipal);

        Assertions.assertEquals(1, response.meta().page());
        Mockito.verify(notificationService).getInbox(1L, false, expectedPageable);
    }

    @Test
    void requestJsonAcceptsEmailChannelAtDeserializationBoundary() throws JsonProcessingException {
        String payload = "{\"title\":\"Email\",\"body\":\"Không hợp lệ\","
                + "\"audienceType\":\"SCHOOL\",\"channel\":\"EMAIL\"}";

        ReqCreateNotificationDTO request = new ObjectMapper().readValue(payload, ReqCreateNotificationDTO.class);

        Assertions.assertEquals(NotificationChannel.EMAIL, request.getChannel());
    }

    @Test
    void notificationMutationsAllowTeacherRole() throws NoSuchMethodException {
        Assertions.assertEquals(
                "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')",
                NotificationController.class.getMethod(
                        "create", ReqCreateNotificationDTO.class, UserPrincipal.class)
                        .getAnnotation(PreAuthorize.class).value());
        Assertions.assertEquals(
                "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')",
                NotificationController.class.getMethod(
                        "publish", Long.class, ReqPublishNotificationDTO.class, UserPrincipal.class)
                        .getAnnotation(PreAuthorize.class).value());
        Assertions.assertEquals(
                "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE', 'TEACHER')",
                NotificationController.class.getMethod(
                        "cancel", Long.class, UserPrincipal.class)
                        .getAnnotation(PreAuthorize.class).value());
    }
}
