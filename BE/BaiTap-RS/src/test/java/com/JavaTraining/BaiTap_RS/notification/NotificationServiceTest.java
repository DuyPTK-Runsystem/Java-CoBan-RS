package com.JavaTraining.BaiTap_RS.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqCreateNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqPublishNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationReceiptDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationAudienceType;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationChannel;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationReceipt;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationStatus;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationIndividualAudienceProjectionRepository;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationRepository;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationReceiptRepository;
import com.JavaTraining.BaiTap_RS.notification.service.NotificationAudienceService;
import com.JavaTraining.BaiTap_RS.notification.service.NotificationAuditService;
import com.JavaTraining.BaiTap_RS.notification.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
                "PMD.AvoidDuplicateLiterals",
                "PMD.UnitTestAssertionsShouldIncludeMessage",
                "PMD.UnitTestContainsTooManyAsserts",
                "PMD.TooManyMethods",
                "PMD.ExcessiveImports"
})
class NotificationServiceTest {

        @Mock
        private NotificationRepository notificationRepository;

        @Mock
        private NotificationReceiptRepository notificationReceiptRepository;

        @Mock
        private NotificationAudienceService notificationAudienceService;

        @Mock
        private NotificationAuditService notificationAuditService;

        @Mock
        private SchoolClassRepository schoolClassRepository;

        @Mock
        private NotificationIndividualAudienceProjectionRepository individualAudienceProjectionRepository;

        private NotificationService notificationService;

        private NotificationService managerNotificationService() {
                return new NotificationService(
                                notificationRepository,
                                notificationReceiptRepository,
                                notificationAudienceService,
                                notificationAuditService,
                                schoolClassRepository,
                                individualAudienceProjectionRepository);
        }

        @BeforeEach
        void setUp() {
                notificationService = new NotificationService(
                                notificationRepository,
                                notificationReceiptRepository,
                                notificationAudienceService,
                                notificationAuditService);
        }

        private Notification createAndCaptureDraft(ReqCreateNotificationDTO request, Long actorUserId) {
                Mockito.when(notificationRepository.saveAndFlush(ArgumentMatchers.any(Notification.class)))
                                .thenAnswer(invocation -> {
                                        Notification saved = invocation.getArgument(0);
                                        ReflectionTestUtils.setField(saved, "id", 99L);
                                        return saved;
                                });

                notificationService.createDraft(request, actorUserId);

                ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
                Mockito.verify(notificationRepository).saveAndFlush(captor.capture());
                return captor.getValue();
        }

        @Test
        void createDraftSuccess() {
                ReqCreateNotificationDTO request = ReqCreateNotificationDTO.builder()
                                .title("Thông báo họp phụ huynh")
                                .body("Kính mời phụ huynh tham gia họp vào sáng Chủ nhật.")
                                .audienceType(NotificationAudienceType.CLASS)
                                .targetReference("101")
                                .schoolScope("DEFAULT_SCHOOL")
                                .build();

                Notification saved = new Notification(
                                request.getTitle(),
                                request.getBody(),
                                request.getAudienceType(),
                                request.getTargetReference(),
                                1L,
                                "DEFAULT_SCHOOL");
                ReflectionTestUtils.setField(saved, "id", 10L);

                Mockito.when(notificationRepository.saveAndFlush(ArgumentMatchers.any(Notification.class)))
                                .thenReturn(saved);

                ResNotificationDTO result = notificationService.createDraft(request, 1L);

                Assertions.assertNotNull(result);
                Assertions.assertEquals(10L, result.getId());
                Assertions.assertEquals("Thông báo họp phụ huynh", result.getTitle());
                Assertions.assertEquals(NotificationChannel.IN_APP, result.getChannel());
                Assertions.assertEquals(NotificationStatus.DRAFT, result.getStatus());
                Mockito.verify(notificationAuditService).auditCreateDraft(1L, saved);
        }

        @Test
        void v3ChannelEnumExposesInAppOnly() {
                Assertions.assertArrayEquals(
                                new NotificationChannel[] {NotificationChannel.IN_APP},
                                NotificationChannel.values());
        }

        @Test
        void createDraftRejectsScheduledPublishAt() {
                ReqCreateNotificationDTO request = ReqCreateNotificationDTO.builder()
                                .title("Không hỗ trợ schedule")
                                .body("Slice hiện tại chỉ publish ngay")
                                .audienceType(NotificationAudienceType.SCHOOL)
                                .publishAt(LocalDateTime.now().plusHours(1))
                                .build();

                AppException ex = Assertions.assertThrows(
                                AppException.class, () -> notificationService.createDraft(request, 1L));

                Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
                Mockito.verify(notificationRepository, Mockito.never()).save(ArgumentMatchers.any(Notification.class));
        }

        @Test
        void createDraftRejectsExpiryBeforePublishAt() {
                ReqCreateNotificationDTO request = ReqCreateNotificationDTO.builder()
                                .title("Khoảng thời gian không hợp lệ")
                                .body("expiresAt phải sau publishAt")
                                .audienceType(NotificationAudienceType.SCHOOL)
                                .publishAt(LocalDateTime.now().plusHours(1))
                                .expiresAt(LocalDateTime.now().plusMinutes(30))
                                .build();

                AppException ex = Assertions.assertThrows(
                                AppException.class, () -> notificationService.createDraft(request, 1L));

                Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
                Mockito.verify(notificationRepository, Mockito.never()).save(ArgumentMatchers.any(Notification.class));
        }

        @Test
        void createDraftIdempotencyDuplicateReturnsExisting() {
                ReqCreateNotificationDTO request = ReqCreateNotificationDTO.builder()
                                .title("Thông báo kiểm tra")
                                .body("Nội dung kiểm tra")
                                .audienceType(NotificationAudienceType.SCHOOL)
                                .idempotencyKey("KEY-123")
                                .schoolScope("DEFAULT_SCHOOL")
                                .build();

                Notification existing = createAndCaptureDraft(request, 1L);
                ReflectionTestUtils.setField(existing, "id", 5L);

                Mockito.reset(notificationRepository);
                Mockito.when(notificationRepository.findByIdempotencyKey("KEY-123")).thenReturn(Optional.of(existing));

                ResNotificationDTO result = notificationService.createDraft(request, 1L);

                Assertions.assertEquals(5L, result.getId());
                Mockito.verify(notificationRepository, Mockito.never()).saveAndFlush(ArgumentMatchers.any());
        }

        @Test
        void createDraftIdempotencyPayloadMismatchThrowsConflict() {
                ReqCreateNotificationDTO original = ReqCreateNotificationDTO.builder()
                                .title("Tiêu đề cũ")
                                .body("Nội dung cũ")
                                .audienceType(NotificationAudienceType.SCHOOL)
                                .idempotencyKey("KEY-123")
                                .schoolScope("DEFAULT_SCHOOL")
                                .build();
                Notification existing = createAndCaptureDraft(original, 1L);

                Mockito.reset(notificationRepository);
                ReqCreateNotificationDTO request = ReqCreateNotificationDTO.builder()
                                .title("Tiêu đề mới")
                                .body("Nội dung mới")
                                .audienceType(original.getAudienceType())
                                .idempotencyKey(original.getIdempotencyKey())
                                .schoolScope(original.getSchoolScope())
                                .build();

                Mockito.when(notificationRepository.findByIdempotencyKey("KEY-123")).thenReturn(Optional.of(existing));

                AppException ex = Assertions.assertThrows(
                                AppException.class, () -> notificationService.createDraft(request, 1L));
                Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        }

        @Test
        void createDraftSameKeySamePayloadReturnsExistingForReorderedDuplicateRecipients() {
                LocalDateTime expiry = LocalDateTime.of(2026, 10, 1, 12, 0);
                ReqCreateNotificationDTO original = ReqCreateNotificationDTO.builder()
                                .title("Thông báo cá nhân")
                                .body("Nội dung cá nhân")
                                .audienceType(NotificationAudienceType.INDIVIDUAL)
                                .recipientUserIds(List.of(3L, 2L, 2L))
                                .channel(null)
                                .expiresAt(expiry)
                                .idempotencyKey("KEY-RECIPIENTS")
                                .schoolScope("DEFAULT_SCHOOL")
                                .build();
                Notification existing = createAndCaptureDraft(original, 1L);

                Mockito.reset(notificationRepository);
                ReqCreateNotificationDTO retry = ReqCreateNotificationDTO.builder()
                                .title(original.getTitle())
                                .body(original.getBody())
                                .audienceType(original.getAudienceType())
                                .recipientUserIds(List.of(2L, 3L))
                                .channel(NotificationChannel.IN_APP)
                                .expiresAt(expiry)
                                .idempotencyKey(original.getIdempotencyKey())
                                .schoolScope(original.getSchoolScope())
                                .build();
                Mockito.when(notificationRepository.findByIdempotencyKey("KEY-RECIPIENTS"))
                                .thenReturn(Optional.of(existing));

                ResNotificationDTO result = notificationService.createDraft(retry, 1L);

                Assertions.assertEquals(existing.getId(), result.getId());
                Mockito.verify(notificationRepository, Mockito.never()).saveAndFlush(ArgumentMatchers.any());
        }

        @Test
        void createDraftSameKeyDifferentTargetThrowsConflict() {
                ReqCreateNotificationDTO original = ReqCreateNotificationDTO.builder()
                                .title("Thông báo lớp")
                                .body("Nội dung lớp")
                                .audienceType(NotificationAudienceType.CLASS)
                                .targetReference("101")
                                .idempotencyKey("KEY-TARGET")
                                .schoolScope("DEFAULT_SCHOOL")
                                .build();
                Notification existing = createAndCaptureDraft(original, 1L);

                Mockito.reset(notificationRepository);
                ReqCreateNotificationDTO retry = ReqCreateNotificationDTO.builder()
                                .title(original.getTitle())
                                .body(original.getBody())
                                .audienceType(original.getAudienceType())
                                .targetReference("102")
                                .idempotencyKey(original.getIdempotencyKey())
                                .schoolScope(original.getSchoolScope())
                                .build();
                Mockito.when(notificationRepository.findByIdempotencyKey("KEY-TARGET"))
                                .thenReturn(Optional.of(existing));

                AppException ex = Assertions.assertThrows(
                                AppException.class, () -> notificationService.createDraft(retry, 1L));

                Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        }

        @Test
        void createDraftRejectsNonDefaultSchoolScope() {
                ReqCreateNotificationDTO request = ReqCreateNotificationDTO.builder()
                                .title("Thông báo trường")
                                .body("Nội dung trường")
                                .audienceType(NotificationAudienceType.SCHOOL)
                                .schoolScope("OTHER_SCHOOL")
                                .build();

                AppException ex = Assertions.assertThrows(
                                AppException.class, () -> notificationService.createDraft(request, 1L));

                Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        }

        @Test
        void createDraftRejectsBlankSchoolScope() {
                ReqCreateNotificationDTO request = ReqCreateNotificationDTO.builder()
                                .title("Thiếu scope")
                                .body("Không được tạo")
                                .audienceType(NotificationAudienceType.SCHOOL)
                                .schoolScope(" ")
                                .build();

                AppException ex = Assertions.assertThrows(
                                AppException.class, () -> notificationService.createDraft(request, 1L));

                Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        }

        @Test
        void createDraftRejectsTargetReferenceForSchoolAudience() {
                ReqCreateNotificationDTO request = ReqCreateNotificationDTO.builder()
                                .title("Thông báo trường")
                                .body("Nội dung trường")
                                .audienceType(NotificationAudienceType.SCHOOL)
                                .targetReference("101")
                                .schoolScope("DEFAULT_SCHOOL")
                                .build();

                AppException ex = Assertions.assertThrows(
                                AppException.class, () -> notificationService.createDraft(request, 1L));

                Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
                Mockito.verify(notificationRepository, Mockito.never())
                                .saveAndFlush(ArgumentMatchers.any(Notification.class));
        }

        @Test
        void createDraftRejectsRecipientIdsForSchoolAudience() {
                ReqCreateNotificationDTO request = ReqCreateNotificationDTO.builder()
                                .title("Thông báo trường")
                                .body("Nội dung trường")
                                .audienceType(NotificationAudienceType.SCHOOL)
                                .recipientUserIds(List.of(2L))
                                .schoolScope("DEFAULT_SCHOOL")
                                .build();

                AppException ex = Assertions.assertThrows(
                                AppException.class, () -> notificationService.createDraft(request, 1L));

                Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        }

        @Test
        void createDraftRejectsRecipientIdsForClassAudience() {
                ReqCreateNotificationDTO request = ReqCreateNotificationDTO.builder()
                                .title("Thông báo lớp")
                                .body("Nội dung lớp")
                                .audienceType(NotificationAudienceType.CLASS)
                                .targetReference("101")
                                .recipientUserIds(List.of(2L))
                                .schoolScope("DEFAULT_SCHOOL")
                                .build();

                AppException ex = Assertions.assertThrows(
                                AppException.class, () -> notificationService.createDraft(request, 1L));

                Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        }

        @Test
        void createDraftRejectsNonNumericClassReference() {
                ReqCreateNotificationDTO request = ReqCreateNotificationDTO.builder()
                                .title("Thông báo lớp")
                                .body("Nội dung lớp")
                                .audienceType(NotificationAudienceType.CLASS)
                                .targetReference("CLASS-101")
                                .schoolScope("DEFAULT_SCHOOL")
                                .build();

                AppException ex = Assertions.assertThrows(
                                AppException.class, () -> notificationService.createDraft(request, 1L));

                Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        }

        @Test
        void createDraftRejectsTargetReferenceForIndividualAudience() {
                ReqCreateNotificationDTO request = ReqCreateNotificationDTO.builder()
                                .title("Thông báo cá nhân")
                                .body("Nội dung cá nhân")
                                .audienceType(NotificationAudienceType.INDIVIDUAL)
                                .targetReference("2")
                                .recipientUserIds(List.of(2L))
                                .schoolScope("DEFAULT_SCHOOL")
                                .build();

                AppException ex = Assertions.assertThrows(
                                AppException.class, () -> notificationService.createDraft(request, 1L));

                Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        }

        @Test
        void createDraftRejectsMissingIndividualRecipients() {
                ReqCreateNotificationDTO request = ReqCreateNotificationDTO.builder()
                                .title("Thông báo cá nhân")
                                .body("Nội dung cá nhân")
                                .audienceType(NotificationAudienceType.INDIVIDUAL)
                                .schoolScope("DEFAULT_SCHOOL")
                                .build();

                AppException ex = Assertions.assertThrows(
                                AppException.class, () -> notificationService.createDraft(request, 1L));

                Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        }

        @Test
        void createDraftRejectsInvalidIndividualRecipientId() {
                ReqCreateNotificationDTO request = ReqCreateNotificationDTO.builder()
                                .title("Thông báo cá nhân")
                                .body("Nội dung cá nhân")
                                .audienceType(NotificationAudienceType.INDIVIDUAL)
                                .recipientUserIds(List.of(0L))
                                .schoolScope("DEFAULT_SCHOOL")
                                .build();

                AppException ex = Assertions.assertThrows(
                                AppException.class, () -> notificationService.createDraft(request, 1L));

                Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
        }

        @Test
        void createDraftMapsIdempotencyConstraintRaceToConflict() {
                ReqCreateNotificationDTO request = ReqCreateNotificationDTO.builder()
                                .title("Thông báo race")
                                .body("Nội dung race")
                                .audienceType(NotificationAudienceType.SCHOOL)
                                .idempotencyKey("KEY-RACE")
                                .schoolScope("DEFAULT_SCHOOL")
                                .build();
                Mockito.when(notificationRepository.saveAndFlush(ArgumentMatchers.any(Notification.class)))
                                .thenThrow(new DataIntegrityViolationException(
                                                "Duplicate entry for constraint uk_notification_idempotency"));

                AppException ex = Assertions.assertThrows(
                                AppException.class, () -> notificationService.createDraft(request, 1L));

                Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatus());
                Assertions.assertInstanceOf(DataIntegrityViolationException.class, ex.getCause());
        }

        @Test
        void publishIndividualAudienceCreatesReceiptsAndSetsPublished() {
                Notification notification = new Notification(
                                "Thông báo cá nhân",
                                "Nội dung gửi cá nhân",
                                NotificationAudienceType.INDIVIDUAL,
                                "2,3",
                                1L,
                                "DEFAULT_SCHOOL");
                ReflectionTestUtils.setField(notification, "id", 15L);

                Mockito.when(notificationRepository.findById(15L)).thenReturn(Optional.of(notification));
                Mockito.when(notificationAudienceService.resolveAudienceUserIds(notification, null))
                                .thenReturn(Set.of(2L, 3L));
                Mockito.when(notificationReceiptRepository.existsByNotificationIdAndRecipientUserId(15L, 2L))
                                .thenReturn(false);
                Mockito.when(notificationReceiptRepository.existsByNotificationIdAndRecipientUserId(15L, 3L))
                                .thenReturn(false);
                Mockito.when(notificationRepository.save(ArgumentMatchers.any(Notification.class)))
                                .thenReturn(notification);

                ReqPublishNotificationDTO request = ReqPublishNotificationDTO.builder().build();
                ResNotificationDTO res = notificationService.publish(15L, request, 1L);

                Assertions.assertNotNull(res);
                Assertions.assertEquals(NotificationStatus.PUBLISHED, notification.getStatus());
                Mockito.verify(notificationReceiptRepository).saveAll(ArgumentMatchers.anyList());
                Mockito.verify(notificationAuditService).auditPublish(1L, notification, 2);
        }

        @Test
        void publishClassAudienceSuccess() {
                Notification notification = new Notification(
                                "Thông báo lớp 10A1",
                                "Lớp nghỉ học ngày mai",
                                NotificationAudienceType.CLASS,
                                "201",
                                1L,
                                "DEFAULT_SCHOOL");
                ReflectionTestUtils.setField(notification, "id", 20L);

                Mockito.when(notificationRepository.findById(20L)).thenReturn(Optional.of(notification));
                Mockito.when(notificationAudienceService.resolveAudienceUserIds(notification, null))
                                .thenReturn(Set.of(100L, 101L));
                Mockito.when(notificationRepository.save(ArgumentMatchers.any(Notification.class)))
                                .thenReturn(notification);

                ResNotificationDTO res = notificationService.publish(20L, null, 1L);

                Assertions.assertEquals(NotificationStatus.PUBLISHED, res.getStatus());
        }

        @Test
        void publishSchoolAudienceSuccess() {
                Notification notification = new Notification(
                                "Thông báo toàn trường",
                                "Khai giảng năm học mới",
                                NotificationAudienceType.SCHOOL,
                                null,
                                1L,
                                "DEFAULT_SCHOOL");
                ReflectionTestUtils.setField(notification, "id", 30L);

                Mockito.when(notificationRepository.findById(30L)).thenReturn(Optional.of(notification));
                Mockito.when(notificationAudienceService.resolveAudienceUserIds(notification, null))
                                .thenReturn(Set.of(1L, 2L, 3L));
                Mockito.when(notificationRepository.save(ArgumentMatchers.any(Notification.class)))
                                .thenReturn(notification);

                ResNotificationDTO res = notificationService.publish(30L, null, 1L);

                Assertions.assertEquals(NotificationStatus.PUBLISHED, res.getStatus());
        }

        @Test
        void publishAlreadyPublishedIsIdempotent() {
                Notification notification = new Notification(
                                "Đã xuất bản",
                                "Nội dung",
                                NotificationAudienceType.SCHOOL,
                                null,
                                1L,
                                "DEFAULT_SCHOOL");
                notification.setStatus(NotificationStatus.PUBLISHED);
                ReflectionTestUtils.setField(notification, "id", 35L);

                Mockito.when(notificationRepository.findById(35L)).thenReturn(Optional.of(notification));

                ResNotificationDTO res = notificationService.publish(35L, null, 1L);

                Assertions.assertEquals(NotificationStatus.PUBLISHED, res.getStatus());
                Mockito.verify(notificationReceiptRepository, Mockito.never()).saveAll(ArgumentMatchers.anyList());
        }

        @Test
        void publishCancelledThrowsConflict() {
                Notification notification = new Notification(
                                "Đã hủy",
                                "Nội dung",
                                NotificationAudienceType.SCHOOL,
                                null,
                                1L,
                                "DEFAULT_SCHOOL");
                notification.setStatus(NotificationStatus.CANCELLED);
                ReflectionTestUtils.setField(notification, "id", 36L);

                Mockito.when(notificationRepository.findById(36L)).thenReturn(Optional.of(notification));

                AppException ex = Assertions.assertThrows(AppException.class,
                                () -> notificationService.publish(36L, null, 1L));
                Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        }

        @Test
        void publishVersionMismatchThrowsConflict() {
                Notification notification = new Notification(
                                "Thông báo",
                                "Nội dung",
                                NotificationAudienceType.SCHOOL,
                                null,
                                1L,
                                "DEFAULT_SCHOOL");
                ReflectionTestUtils.setField(notification, "id", 37L);
                ReflectionTestUtils.setField(notification, "version", 2L);

                Mockito.when(notificationRepository.findById(37L)).thenReturn(Optional.of(notification));

                ReqPublishNotificationDTO request = ReqPublishNotificationDTO.builder().expectedVersion(1L).build();
                AppException ex = Assertions.assertThrows(
                                AppException.class, () -> notificationService.publish(37L, request, 1L));
                Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        }

        @Test
        void teacherCannotPublishAnotherSenderNotification() {
                Notification notification = new Notification(
                                "Thông báo của người khác",
                                "Nội dung",
                                NotificationAudienceType.SCHOOL,
                                null,
                                1L,
                                "DEFAULT_SCHOOL");
                ReflectionTestUtils.setField(notification, "id", 42L);
                Mockito.when(notificationRepository.findById(42L)).thenReturn(Optional.of(notification));

                AppException ex = Assertions.assertThrows(
                                AppException.class,
                                () -> notificationService.publish(42L, null, 2L, false));

                Assertions.assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
                Mockito.verifyNoInteractions(notificationAudienceService, notificationReceiptRepository);
        }

        @Test
        void publishExpiredTimestampThrowsUnprocessableEntity() {
                Notification notification = new Notification(
                                "Thông báo hết hạn",
                                "Không thể phát hành",
                                NotificationAudienceType.SCHOOL,
                                null,
                                1L,
                                "DEFAULT_SCHOOL");
                notification.setExpiresAt(LocalDateTime.now().minusMinutes(1));
                ReflectionTestUtils.setField(notification, "id", 38L);

                Mockito.when(notificationRepository.findById(38L)).thenReturn(Optional.of(notification));

                AppException ex = Assertions.assertThrows(
                                AppException.class, () -> notificationService.publish(38L, null, 1L));

                Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
                Mockito.verify(notificationAudienceService, Mockito.never())
                                .resolveAudienceUserIds(Mockito.any(), Mockito.any());
                Mockito.verify(notificationReceiptRepository, Mockito.never()).saveAll(Mockito.anyList());
        }

        @Test
        void publishRejectsMalformedStoredAudienceContract() {
                Notification notification = new Notification(
                                "Thông báo lỗi",
                                "Không được phát hành",
                                NotificationAudienceType.SCHOOL,
                                "101",
                                1L,
                                "DEFAULT_SCHOOL");
                ReflectionTestUtils.setField(notification, "id", 39L);

                Mockito.when(notificationRepository.findById(39L)).thenReturn(Optional.of(notification));

                AppException ex = Assertions.assertThrows(
                                AppException.class, () -> notificationService.publish(39L, null, 1L));

                Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
                Mockito.verify(notificationAudienceService, Mockito.never())
                                .resolveAudienceUserIds(Mockito.any(), Mockito.any());
        }

        @Test
        void cancelDraftSuccess() {
                Notification notification = new Notification(
                                "Dự thảo",
                                "Nội dung",
                                NotificationAudienceType.CLASS,
                                "10",
                                1L,
                                "DEFAULT_SCHOOL");
                ReflectionTestUtils.setField(notification, "id", 40L);

                Mockito.when(notificationRepository.findById(40L)).thenReturn(Optional.of(notification));
                Mockito.when(notificationRepository.save(ArgumentMatchers.any(Notification.class)))
                                .thenReturn(notification);

                ResNotificationDTO res = notificationService.cancel(40L, 1L);

                Assertions.assertEquals(NotificationStatus.CANCELLED, res.getStatus());
                Mockito.verify(notificationAuditService).auditCancel(1L, notification);
        }

        @Test
        void cancelPublishedThrowsConflict() {
                Notification notification = new Notification(
                                "Đã xuất bản",
                                "Nội dung",
                                NotificationAudienceType.CLASS,
                                "10",
                                1L,
                                "DEFAULT_SCHOOL");
                notification.setStatus(NotificationStatus.PUBLISHED);
                ReflectionTestUtils.setField(notification, "id", 41L);

                Mockito.when(notificationRepository.findById(41L)).thenReturn(Optional.of(notification));

                AppException ex = Assertions.assertThrows(AppException.class,
                                () -> notificationService.cancel(41L, 1L));
                Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        }

        @Test
        void teacherCannotCancelAnotherSenderNotification() {
                Notification notification = new Notification(
                                "Thông báo của người khác",
                                "Nội dung",
                                NotificationAudienceType.SCHOOL,
                                null,
                                1L,
                                "DEFAULT_SCHOOL");
                ReflectionTestUtils.setField(notification, "id", 43L);
                Mockito.when(notificationRepository.findById(43L)).thenReturn(Optional.of(notification));

                AppException ex = Assertions.assertThrows(
                                AppException.class,
                                () -> notificationService.cancel(43L, 2L, false));

                Assertions.assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
                Mockito.verify(notificationRepository, Mockito.never()).save(Mockito.any(Notification.class));
        }

        @Test
        void inboxReturnsPagedDTOWithReadState() {
                Pageable pageable = PageRequest.of(0, 20);
                NotificationReceipt receipt = new NotificationReceipt(50L, 5L, "INDIVIDUAL");
                ReflectionTestUtils.setField(receipt, "id", 1L);
                receipt.setReadAt(LocalDateTime.now());

                Page<NotificationReceipt> page = new PageImpl<>(List.of(receipt), pageable, 41);
                Mockito.when(notificationReceiptRepository.findVisibleByRecipientUserIdOrderByCreatedAtDesc(5L, pageable))
                                .thenReturn(page);

                Notification notification = new Notification(
                                "Tin nhắn inbox",
                                "Nội dung inbox",
                                NotificationAudienceType.INDIVIDUAL,
                                "5",
                                1L,
                                "DEFAULT_SCHOOL");
                notification.setStatus(NotificationStatus.PUBLISHED);
                notification.setPublishAt(LocalDateTime.now().minusMinutes(1));
                ReflectionTestUtils.setField(notification, "id", 50L);

                Mockito.when(notificationRepository.findAllById(List.of(50L))).thenReturn(List.of(notification));

                ResultPaginationDTO<ResNotificationDTO> res = notificationService.getInbox(5L, false, pageable);

                Assertions.assertNotNull(res);
                Assertions.assertEquals(1, res.result().size());
                Assertions.assertEquals("Tin nhắn inbox", res.result().get(0).getTitle());
                Assertions.assertTrue(res.result().get(0).getRead());
                Assertions.assertEquals(3, res.meta().totalPages());
                Assertions.assertEquals(41, res.meta().totalItems());
        }

        @Test
        void inboxDoesNotIncludeScheduledNotificationBeforePublishAt() {
                Pageable pageable = PageRequest.of(0, 20);
                NotificationReceipt receipt = new NotificationReceipt(51L, 5L, "SCHOOL");
                Notification notification = new Notification(
                                "Thông báo chưa đến giờ",
                                "Chỉ hiển thị sau publishAt",
                                NotificationAudienceType.SCHOOL,
                                null,
                                1L,
                                "DEFAULT_SCHOOL");
                notification.setStatus(NotificationStatus.SCHEDULED);
                notification.setPublishAt(LocalDateTime.now().plusHours(1));
                ReflectionTestUtils.setField(notification, "id", 51L);

                Mockito.when(notificationReceiptRepository.findVisibleByRecipientUserIdOrderByCreatedAtDesc(5L, pageable))
                                .thenReturn(new PageImpl<>(List.of(receipt), pageable, 1));
                Mockito.when(notificationRepository.findAllById(List.of(51L))).thenReturn(List.of(notification));

                ResultPaginationDTO<ResNotificationDTO> res = notificationService.getInbox(5L, false, pageable);

                Assertions.assertTrue(res.result().isEmpty());
        }

        @Test
        void inboxDoesNotIncludeCancelledNotification() {
                Pageable pageable = PageRequest.of(0, 20);
                NotificationReceipt receipt = new NotificationReceipt(52L, 5L, "SCHOOL");
                Notification notification = new Notification(
                                "Thông báo đã hủy",
                                "Không được hiển thị cho người nhận",
                                NotificationAudienceType.SCHOOL,
                                null,
                                1L,
                                "DEFAULT_SCHOOL");
                notification.setStatus(NotificationStatus.CANCELLED);
                ReflectionTestUtils.setField(notification, "id", 52L);

                Mockito.when(notificationReceiptRepository.findVisibleByRecipientUserIdOrderByCreatedAtDesc(5L, pageable))
                                .thenReturn(new PageImpl<>(List.of(receipt), pageable, 1));
                Mockito.when(notificationRepository.findAllById(List.of(52L))).thenReturn(List.of(notification));

                ResultPaginationDTO<ResNotificationDTO> res = notificationService.getInbox(5L, false, pageable);

                Assertions.assertTrue(res.result().isEmpty());
        }

        @Test
        void inboxDoesNotIncludeExpiredNotification() {
                Pageable pageable = PageRequest.of(0, 20);
                NotificationReceipt receipt = new NotificationReceipt(53L, 5L, "SCHOOL");
                Notification notification = new Notification(
                                "Thông báo đã hết hạn",
                                "Không được hiển thị như active",
                                NotificationAudienceType.SCHOOL,
                                null,
                                1L,
                                "DEFAULT_SCHOOL");
                notification.setStatus(NotificationStatus.PUBLISHED);
                notification.setPublishAt(LocalDateTime.now().minusHours(2));
                notification.setExpiresAt(LocalDateTime.now().minusMinutes(1));
                ReflectionTestUtils.setField(notification, "id", 53L);

                Mockito.when(notificationReceiptRepository.findVisibleByRecipientUserIdOrderByCreatedAtDesc(5L, pageable))
                                .thenReturn(new PageImpl<>(List.of(receipt), pageable, 1));
                Mockito.when(notificationRepository.findAllById(List.of(53L))).thenReturn(List.of(notification));

                ResultPaginationDTO<ResNotificationDTO> res = notificationService.getInbox(5L, false, pageable);

                Assertions.assertTrue(res.result().isEmpty());
        }

        @Test
        void detailAccessManagerOrRecipientOrSender() {
                Notification notification = new Notification(
                                "Chi tiết",
                                "Nội dung",
                                NotificationAudienceType.INDIVIDUAL,
                                "9",
                                1L,
                                "DEFAULT_SCHOOL");
                notification.setStatus(NotificationStatus.PUBLISHED);
                notification.setPublishAt(LocalDateTime.now().minusMinutes(1));
                ReflectionTestUtils.setField(notification, "id", 60L);

                Mockito.when(notificationRepository.findById(60L)).thenReturn(Optional.of(notification));
                Mockito.when(notificationReceiptRepository.findByNotificationIdAndRecipientUserId(60L, 9L))
                                .thenReturn(Optional.of(new NotificationReceipt(60L, 9L, "INDIVIDUAL")));

                // Case recipient
                ResNotificationDTO resRecipient = notificationService.getNotificationDetail(60L, 9L, false);
                Assertions.assertNotNull(resRecipient);

                // Case stranger -> 403
                Mockito.when(notificationReceiptRepository.findByNotificationIdAndRecipientUserId(60L, 999L))
                                .thenReturn(Optional.empty());
                AppException ex = Assertions.assertThrows(
                                AppException.class, () -> notificationService.getNotificationDetail(60L, 999L, false));
                Assertions.assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        }

        @Test
        void expiredRecipientDetailIsNotFoundAndDoesNotExposeContent() {
                Notification notification = new Notification(
                                "Tiêu đề đã hết hạn",
                                "Nội dung không được leak",
                                NotificationAudienceType.INDIVIDUAL,
                                "9",
                                1L,
                                "DEFAULT_SCHOOL");
                notification.setStatus(NotificationStatus.PUBLISHED);
                notification.setPublishAt(LocalDateTime.now().minusHours(2));
                notification.setExpiresAt(LocalDateTime.now().minusMinutes(1));
                ReflectionTestUtils.setField(notification, "id", 64L);

                Mockito.when(notificationRepository.findById(64L)).thenReturn(Optional.of(notification));
                Mockito.when(notificationReceiptRepository.findByNotificationIdAndRecipientUserId(64L, 9L))
                                .thenReturn(Optional.of(new NotificationReceipt(64L, 9L, "INDIVIDUAL")));

                AppException ex = Assertions.assertThrows(
                                AppException.class, () -> notificationService.getNotificationDetail(64L, 9L, false));

                Assertions.assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
                Assertions.assertEquals("Không tìm thấy thông báo với ID: 64", ex.getMessage());
        }

        @Test
        void recipientResponsesHideTargetingDetails() throws JsonProcessingException {
                Notification notification = new Notification(
                                "Riêng tư",
                                "Nội dung",
                                NotificationAudienceType.INDIVIDUAL,
                                "9,10",
                                1L,
                                "DEFAULT_SCHOOL");
                notification.setStatus(NotificationStatus.PUBLISHED);
                notification.setPublishAt(LocalDateTime.now().minusMinutes(1));
                notification.setIdempotencyKey("PRIVATE-KEY");
                ReflectionTestUtils.setField(notification, "id", 61L);

                Mockito.when(notificationRepository.findById(61L)).thenReturn(Optional.of(notification));
                Mockito.when(notificationReceiptRepository.findByNotificationIdAndRecipientUserId(61L, 9L))
                                .thenReturn(Optional.of(new NotificationReceipt(61L, 9L, "INDIVIDUAL")));

                ResNotificationDTO response = notificationService.getNotificationDetail(61L, 9L, false);

                Assertions.assertNull(response.getTargetReference());
                Assertions.assertNull(response.getAudienceDetails());
                Assertions.assertNull(response.getSenderId());
                Assertions.assertNull(response.getIdempotencyKey());
                String json = new ObjectMapper()
                                .registerModule(new JavaTimeModule())
                                .writeValueAsString(response);
                Assertions.assertFalse(json.contains("targetReference"));
                Assertions.assertFalse(json.contains("senderId"));
                Assertions.assertFalse(json.contains("idempotencyKey"));
        }

        @Test
        void managerResponsesHideRawTargetReferenceAndExposeClassAudienceDetails() {
                Notification notification = new Notification(
                                "Quản lý",
                                "Nội dung",
                                NotificationAudienceType.CLASS,
                                "101",
                                1L,
                                "DEFAULT_SCHOOL");
                notification.setIdempotencyKey("MANAGER-KEY");
                ReflectionTestUtils.setField(notification, "id", 62L);

                Mockito.when(notificationRepository.findById(62L)).thenReturn(Optional.of(notification));
                SchoolClass schoolClass = Mockito.mock(SchoolClass.class);
                Mockito.when(schoolClass.getClassName()).thenReturn("Lớp 10A1");
                Mockito.when(schoolClassRepository.findById(101L)).thenReturn(Optional.of(schoolClass));
                Mockito.when(notificationReceiptRepository.findByNotificationId(62L))
                                .thenReturn(List.of(
                                                new NotificationReceipt(62L, 9L, "CLASS"),
                                                new NotificationReceipt(62L, 10L, "CLASS")));

                ResNotificationDTO response = managerNotificationService().getNotificationDetail(62L, 1L, true);

                Assertions.assertNull(response.getTargetReference());
                Assertions.assertEquals(1L, response.getSenderId());
                Assertions.assertEquals("MANAGER-KEY", response.getIdempotencyKey());
                Assertions.assertNotNull(response.getAudienceDetails());
                Assertions.assertEquals(NotificationAudienceType.CLASS, response.getAudienceDetails().audienceType());
                Assertions.assertEquals("Lớp 10A1", response.getAudienceDetails().displayLabel());
                Assertions.assertEquals(2, response.getAudienceDetails().recipientCount());
                Assertions.assertTrue(response.getAudienceDetails().displayDataAvailable());
        }

        @Test
        void inboxHidesTargetingDetails() throws JsonProcessingException {
                Pageable pageable = PageRequest.of(0, 20);
                NotificationReceipt receipt = new NotificationReceipt(63L, 9L, "INDIVIDUAL");
                Notification notification = new Notification(
                                "Inbox",
                                "Nội dung",
                                NotificationAudienceType.INDIVIDUAL,
                                "9,10",
                                1L,
                                "DEFAULT_SCHOOL");
                notification.setStatus(NotificationStatus.PUBLISHED);
                notification.setPublishAt(LocalDateTime.now().minusMinutes(1));
                notification.setIdempotencyKey("INBOX-KEY");
                ReflectionTestUtils.setField(notification, "id", 63L);

                Mockito.when(notificationReceiptRepository.findVisibleByRecipientUserIdOrderByCreatedAtDesc(9L, pageable))
                                .thenReturn(new PageImpl<>(List.of(receipt), pageable, 1));
                Mockito.when(notificationRepository.findAllById(List.of(63L))).thenReturn(List.of(notification));

                ResNotificationDTO response = notificationService.getInbox(9L, false, pageable)
                                .result()
                                .get(0);

                Assertions.assertNull(response.getTargetReference());
                Assertions.assertNull(response.getAudienceDetails());
                Assertions.assertNull(response.getSenderId());
                Assertions.assertNull(response.getIdempotencyKey());
                String json = new ObjectMapper()
                                .registerModule(new JavaTimeModule())
                                .writeValueAsString(response);
                Assertions.assertFalse(json.contains("targetReference"));
                Assertions.assertFalse(json.contains("senderId"));
                Assertions.assertFalse(json.contains("idempotencyKey"));
        }

        @Test
        void markAsReadIdempotent() {
                NotificationReceipt receipt = new NotificationReceipt(70L, 10L, "CLASS");
                ReflectionTestUtils.setField(receipt, "id", 100L);

                Mockito.when(notificationReceiptRepository.findByNotificationIdAndRecipientUserId(70L, 10L))
                                .thenReturn(Optional.of(receipt));
                Mockito.when(notificationReceiptRepository.save(ArgumentMatchers.any(NotificationReceipt.class)))
                                .thenReturn(receipt);

                ResNotificationReceiptDTO resFirst = notificationService.markAsRead(70L, 10L);
                Assertions.assertNotNull(resFirst.getReadAt());
                Assertions.assertNull(resFirst.getRecipientUserId());
                Mockito.verify(notificationAuditService).auditMarkRead(10L, 70L);

                // Mark again should not crash and preserve readAt
                ResNotificationReceiptDTO resSecond = notificationService.markAsRead(70L, 10L);
                Assertions.assertNotNull(resSecond.getReadAt());
        }

        @Test
        void markAsReadNotRecipientThrowsForbidden() {
                Mockito.when(notificationReceiptRepository.findByNotificationIdAndRecipientUserId(80L, 12L))
                                .thenReturn(Optional.empty());
                Mockito.when(notificationRepository.existsById(80L)).thenReturn(true);

                AppException ex = Assertions.assertThrows(AppException.class,
                                () -> notificationService.markAsRead(80L, 12L));
                Assertions.assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        }
}
