package com.JavaTraining.BaiTap_RS.academic.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ClassSubjectIncompleteDetail;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSemesterNotificationDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.SemesterCompletenessSummaryDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.SemesterRecipientInfo;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.NotificationChannel;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.NotificationStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterCompletenessNotification;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterCompletenessNotificationRepository;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings({
        "PMD.AvoidCatchingGenericException",
        "PMD.AvoidInstantiatingObjectsInLoops",
        "PMD.GuardLogStatement"
})
public class SemesterNotificationDispatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SemesterNotificationDispatchService.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final SemesterCompletenessNotificationRepository notificationRepository;
    private final SemesterRecipientResolverService recipientResolverService;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public SemesterNotificationDispatchService(
            SemesterCompletenessNotificationRepository notificationRepository,
            SemesterRecipientResolverService recipientResolverService,
            @Autowired(required = false) JavaMailSender mailSender) {
        this.notificationRepository = notificationRepository;
        this.recipientResolverService = recipientResolverService;
        this.mailSender = mailSender;
    }

    @Transactional
    public List<ResSemesterNotificationDTO> dispatchNotifications(
            Long semesterId,
            String checkpointCode,
            Long reportId,
            SemesterCompletenessSummaryDTO summary,
            List<ClassSubjectIncompleteDetail> incompleteDetails) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        SemesterNotificationDispatchService.class,
                        "SemesterNotificationDispatchService.dispatchNotifications");
        List<ResSemesterNotificationDTO> results = new ArrayList<>();
        List<SemesterRecipientInfo> recipients = recipientResolverService.resolveRecipients(
                semesterId, checkpointCode, summary, incompleteDetails);

        for (SemesterRecipientInfo recipient : recipients) {
            Optional<SemesterCompletenessNotification> existingOpt =
                    notificationRepository.findBySemesterIdAndCheckpointCodeAndRecipientEmailAndNotificationChannel(
                            semesterId,
                            checkpointCode,
                            recipient.recipientEmail(),
                            NotificationChannel.EMAIL);

            SemesterCompletenessNotification notification;
            if (existingOpt.isPresent()) {
                notification = existingOpt.get();
                if (notification.getStatus() == NotificationStatus.SENT) {
                    results.add(mapToDto(notification));
                    continue;
                }
            } else {
                notification = new SemesterCompletenessNotification(
                        semesterId,
                        reportId,
                        checkpointCode,
                        recipient.recipientEmail(),
                        recipient.recipientRole(),
                        recipient.recipientTeacherId(),
                        NotificationChannel.EMAIL,
                        NotificationStatus.PENDING,
                        recipient.subject(),
                        recipient.bodyContent());
            }

            deliverNotification(notification);
            notificationRepository.save(notification);
            results.add(mapToDto(notification));
        }

        return results;
    }

    @Transactional
    public List<ResSemesterNotificationDTO> retryFailedNotifications(Long semesterId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SemesterNotificationDispatchService.class,
                "SemesterNotificationDispatchService.retryFailedNotifications");
        List<SemesterCompletenessNotification> failedList =
                notificationRepository.findAllBySemesterIdOrderByCreatedAtDesc(semesterId)
                        .stream()
                        .filter(n -> n.getStatus() == NotificationStatus.FAILED
                                && n.getAttemptCount() < MAX_RETRY_ATTEMPTS)
                        .toList();

        List<ResSemesterNotificationDTO> retried = new ArrayList<>();
        for (SemesterCompletenessNotification notification : failedList) {
            deliverNotification(notification);
            notificationRepository.save(notification);
            retried.add(mapToDto(notification));
        }
        return retried;
    }

    @Transactional(readOnly = true)
    public List<ResSemesterNotificationDTO> getNotificationsForSemester(Long semesterId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SemesterNotificationDispatchService.class,
                "SemesterNotificationDispatchService.getNotificationsForSemester");
        return notificationRepository.findAllBySemesterIdOrderByCreatedAtDesc(semesterId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private void deliverNotification(SemesterCompletenessNotification notification) {
        notification.setAttemptCount(notification.getAttemptCount() + 1);
        if (mailSender == null) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage("Email sender is not configured");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(notification.getRecipientEmail());
            message.setSubject(notification.getSubject());
            message.setText(notification.getBodyContent());
            mailSender.send(message);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notification.setErrorMessage(null);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Gửi thông báo completeness thành công cho notificationId={}", notification.getId());
            }
        } catch (Exception exception) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(exception.getMessage());
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Lỗi khi gửi thông báo notificationId={} ({})",
                        notification.getId(),
                        exception.getClass().getSimpleName());
            }
        }
    }

    public ResSemesterNotificationDTO mapToDto(SemesterCompletenessNotification notification) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SemesterNotificationDispatchService.class,
                "SemesterNotificationDispatchService.mapToDto");
        return new ResSemesterNotificationDTO(
                notification.getId(),
                notification.getSemesterId(),
                notification.getReportId(),
                notification.getCheckpointCode(),
                notification.getRecipientEmail(),
                notification.getRecipientRole(),
                notification.getRecipientTeacherId(),
                notification.getNotificationChannel(),
                notification.getStatus(),
                notification.getSubject(),
                notification.getBodyContent(),
                notification.getAttemptCount(),
                notification.getSentAt(),
                notification.getErrorMessage(),
                notification.getCreatedAt(),
                notification.getUpdatedAt());
    }
}
