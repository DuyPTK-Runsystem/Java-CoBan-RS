package com.JavaTraining.BaiTap_RS.academic.service;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSemesterNotificationDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.SemesterCompletenessSummaryDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.SemesterRecipientInfo;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.NotificationChannel;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.NotificationStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterCompletenessNotification;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterCompletenessNotificationRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({
                "PMD.UnitTestContainsTooManyAsserts",
                "PMD.AvoidDuplicateLiterals"
})
class SemesterNotificationDispatchServiceTest {

        @Mock
        private SemesterCompletenessNotificationRepository notificationRepository;

        @Mock
        private SemesterRecipientResolverService recipientResolverService;

        @Mock
        private JavaMailSender mailSender;

        private SemesterNotificationDispatchService dispatchService;

        @BeforeEach
        void setUp() {
                dispatchService = new SemesterNotificationDispatchService(
                                notificationRepository,
                                recipientResolverService,
                                mailSender);
        }

        @Test
        void dispatchesNotificationSuccessfully() {
                SemesterRecipientInfo recip = new SemesterRecipientInfo(
                                "teacher@school.edu.vn",
                                "SUBJECT_TEACHER",
                                10L,
                                "Thầy A",
                                "Tiêu đề",
                                "Nội dung",
                                List.of("Issue 1"));

                Mockito.when(recipientResolverService.resolveRecipients(Mockito.eq(1L), Mockito.eq("t-7d"),
                                Mockito.any(), Mockito.any())).thenReturn(List.of(recip));

                Mockito.when(notificationRepository
                                .findBySemesterIdAndCheckpointCodeAndRecipientEmailAndNotificationChannel(
                                                1L, "t-7d", "teacher@school.edu.vn", NotificationChannel.EMAIL))
                                .thenReturn(Optional.empty());

                SemesterCompletenessSummaryDTO summary = new SemesterCompletenessSummaryDTO(
                                false, 0, 0, 0, 1, 0, 0, 0, List.of("Issue 1"));

                List<ResSemesterNotificationDTO> dtos = dispatchService.dispatchNotifications(
                                1L, "t-7d", 100L, summary, List.of());

                Assertions.assertEquals(1, dtos.size(), "dtos size should match");
                Mockito.verify(mailSender, Mockito.times(1)).send(Mockito.any(SimpleMailMessage.class));

                ArgumentCaptor<SemesterCompletenessNotification> captor = ArgumentCaptor
                                .forClass(SemesterCompletenessNotification.class);
                Mockito.verify(notificationRepository).save(captor.capture());

                SemesterCompletenessNotification saved = captor.getValue();
                Assertions.assertEquals(NotificationStatus.SENT, saved.getStatus(), "status should be SENT");
                Assertions.assertEquals(1, saved.getAttemptCount(), "attempt count should be 1");
                Assertions.assertNotNull(saved.getSentAt(), "sentAt should not be null");
                Assertions.assertNull(saved.getErrorMessage(), "errorMessage should be null");
        }

        @Test
        void handlesMailSendFailureGracefullyWithoutThrowing() {
                SemesterRecipientInfo recip = new SemesterRecipientInfo(
                                "teacher@school.edu.vn",
                                "SUBJECT_TEACHER",
                                10L,
                                "Thầy A",
                                "Tiêu đề",
                                "Nội dung",
                                List.of("Issue 1"));

                Mockito.when(recipientResolverService.resolveRecipients(Mockito.eq(1L), Mockito.eq("t-7d"),
                                Mockito.any(), Mockito.any())).thenReturn(List.of(recip));

                Mockito.when(notificationRepository
                                .findBySemesterIdAndCheckpointCodeAndRecipientEmailAndNotificationChannel(
                                                1L, "t-7d", "teacher@school.edu.vn", NotificationChannel.EMAIL))
                                .thenReturn(Optional.empty());

                Mockito.doThrow(new MailSendException("SMTP connection timeout"))
                                .when(mailSender).send(Mockito.any(SimpleMailMessage.class));

                SemesterCompletenessSummaryDTO summary = new SemesterCompletenessSummaryDTO(
                                false, 0, 0, 0, 1, 0, 0, 0, List.of("Issue 1"));

                List<ResSemesterNotificationDTO> dtos = dispatchService.dispatchNotifications(
                                1L, "t-7d", 100L, summary, List.of());

                Assertions.assertEquals(1, dtos.size(), "dtos size should match");
                ArgumentCaptor<SemesterCompletenessNotification> captor = ArgumentCaptor
                                .forClass(SemesterCompletenessNotification.class);
                Mockito.verify(notificationRepository).save(captor.capture());

                SemesterCompletenessNotification saved = captor.getValue();
                Assertions.assertEquals(NotificationStatus.FAILED, saved.getStatus(), "status should be FAILED");
                Assertions.assertTrue(saved.getErrorMessage().contains("SMTP connection timeout"),
                                "error message should contain timeout");
        }

        @Test
        void skipsSendingWhenAlreadySentIdempotent() {
                SemesterRecipientInfo recip = new SemesterRecipientInfo(
                                "teacher@school.edu.vn",
                                "SUBJECT_TEACHER",
                                10L,
                                "Thầy A",
                                "Tiêu đề",
                                "Nội dung",
                                List.of("Issue 1"));

                Mockito.when(recipientResolverService.resolveRecipients(Mockito.eq(1L), Mockito.eq("t-7d"),
                                Mockito.any(), Mockito.any())).thenReturn(List.of(recip));

                SemesterCompletenessNotification existing = new SemesterCompletenessNotification(
                                1L, 100L, "t-7d", "teacher@school.edu.vn", "SUBJECT_TEACHER", 10L,
                                NotificationChannel.EMAIL, NotificationStatus.SENT, "Tiêu đề", "Nội dung");

                Mockito.when(notificationRepository
                                .findBySemesterIdAndCheckpointCodeAndRecipientEmailAndNotificationChannel(
                                                1L, "t-7d", "teacher@school.edu.vn", NotificationChannel.EMAIL))
                                .thenReturn(Optional.of(existing));

                SemesterCompletenessSummaryDTO summary = new SemesterCompletenessSummaryDTO(
                                false, 0, 0, 0, 1, 0, 0, 0, List.of("Issue 1"));

                List<ResSemesterNotificationDTO> dtos = dispatchService.dispatchNotifications(
                                1L, "t-7d", 100L, summary, List.of());

                Assertions.assertEquals(1, dtos.size(), "dtos size should match");
                Mockito.verify(mailSender, Mockito.never()).send(Mockito.any(SimpleMailMessage.class));
                Mockito.verify(notificationRepository, Mockito.never()).save(Mockito.any());
        }

        @Test
        void retriesFailedNotifications() {
                SemesterCompletenessNotification failedNotif = new SemesterCompletenessNotification(
                                1L, 100L, "t-7d", "teacher@school.edu.vn", "SUBJECT_TEACHER", 10L,
                                NotificationChannel.EMAIL, NotificationStatus.FAILED, "Tiêu đề", "Nội dung");
                failedNotif.setAttemptCount(1);

                Mockito.when(notificationRepository.findAllBySemesterIdOrderByCreatedAtDesc(1L))
                                .thenReturn(List.of(failedNotif));

                List<ResSemesterNotificationDTO> retried = dispatchService.retryFailedNotifications(1L);

                Assertions.assertEquals(1, retried.size(), "retried size should match");
                Assertions.assertEquals(NotificationStatus.SENT, failedNotif.getStatus(), "status should be SENT");
                Assertions.assertEquals(2, failedNotif.getAttemptCount(), "attempt count should be 2");
                Mockito.verify(notificationRepository).save(failedNotif);
        }

        @Test
        void marksNotificationFailedWhenEmailSenderIsNotConfigured() {
                SemesterNotificationDispatchService serviceWithoutMail =
                                new SemesterNotificationDispatchService(
                                                notificationRepository,
                                                recipientResolverService,
                                                null);
                SemesterRecipientInfo recip = new SemesterRecipientInfo(
                                "teacher@school.edu.vn",
                                "SUBJECT_TEACHER",
                                10L,
                                "Tháº§y A",
                                "TiÃªu Ä‘á»",
                                "Ná»™i dung",
                                List.of("Issue 1"));

                Mockito.when(recipientResolverService.resolveRecipients(Mockito.eq(1L), Mockito.eq("t-7d"),
                                Mockito.any(), Mockito.any())).thenReturn(List.of(recip));
                Mockito.when(notificationRepository
                                .findBySemesterIdAndCheckpointCodeAndRecipientEmailAndNotificationChannel(
                                                1L, "t-7d", "teacher@school.edu.vn", NotificationChannel.EMAIL))
                                .thenReturn(Optional.empty());

                List<ResSemesterNotificationDTO> dtos = serviceWithoutMail.dispatchNotifications(
                                1L,
                                "t-7d",
                                100L,
                                new SemesterCompletenessSummaryDTO(false, 0, 0, 0, 1, 0, 0, 0, List.of("Issue 1")),
                                List.of());

                Assertions.assertEquals(1, dtos.size(), "dtos size should match");
                ArgumentCaptor<SemesterCompletenessNotification> captor = ArgumentCaptor
                                .forClass(SemesterCompletenessNotification.class);
                Mockito.verify(notificationRepository).save(captor.capture());
                Assertions.assertEquals(NotificationStatus.FAILED, captor.getValue().getStatus(),
                                "unconfigured email sender must not be reported as sent");
        }
}
