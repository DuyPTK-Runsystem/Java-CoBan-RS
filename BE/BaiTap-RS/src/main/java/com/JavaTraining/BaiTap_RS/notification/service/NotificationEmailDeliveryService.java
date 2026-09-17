package com.JavaTraining.BaiTap_RS.notification.service;

import java.time.LocalDateTime;
import java.util.List;

import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationChannel;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationDeliveryStatus;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationReceipt;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationEmailDeliveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationEmailDeliveryService.class);

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final String fromEmail;

    @Autowired
    public NotificationEmailDeliveryService(
            TeacherRepository teacherRepository,
            UserRepository userRepository,
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String fromEmail) {
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    public void deliverIfRequired(Notification notification, List<NotificationReceipt> receipts) {
        if (notification.getChannel() != NotificationChannel.EMAIL) {
            return;
        }

        SimpleMailMessage message = createMessage(notification);
        for (NotificationReceipt receipt : receipts) {
            deliver(message, receipt);
        }
    }

    private SimpleMailMessage createMessage(Notification notification) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (fromEmail != null && !fromEmail.isBlank()) {
            message.setFrom(fromEmail);
        }
        message.setSubject(notification.getTitle());
        message.setText(notification.getBody());
        return message;
    }

    private void deliver(SimpleMailMessage message, NotificationReceipt receipt) {
        receipt.setDeliveryStatus(NotificationDeliveryStatus.PENDING);
        if (mailSender == null) {
            markDeliveryFailed(receipt, "Email sender chưa được cấu hình");
            return;
        }

        String recipientEmail = resolveRecipientEmail(receipt.getRecipientUserId());
        if (recipientEmail == null) {
            markDeliveryFailed(receipt, "Người nhận chưa có email hợp lệ");
            return;
        }

        try {
            message.setTo(recipientEmail);
            mailSender.send(message);
            receipt.setDeliveryStatus(NotificationDeliveryStatus.SENT);
            receipt.setDeliveryError(null);
            receipt.setDeliveredAt(LocalDateTime.now());
        } catch (MailException exception) {
            markDeliveryFailed(receipt, "Gửi email thất bại: " + safeMessage(exception));
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(
                        "Không gửi được notification email cho recipientUserId={} ({})",
                        receipt.getRecipientUserId(),
                        exception.getClass().getSimpleName());
            }
        }
    }

    private void markDeliveryFailed(NotificationReceipt receipt, String error) {
        receipt.setDeliveryStatus(NotificationDeliveryStatus.FAILED);
        receipt.setDeliveryError(error);
        receipt.setDeliveredAt(null);
    }

    private String resolveRecipientEmail(Long recipientUserId) {
        Teacher teacher = teacherRepository.findByUserId(recipientUserId).orElse(null);
        if (teacher != null && isEmail(teacher.getEmail())) {
            return teacher.getEmail().trim();
        }

        User user = userRepository.findById(recipientUserId).orElse(null);
        if (user != null && isEmail(user.getUsername())) {
            return user.getUsername().trim();
        }
        return null;
    }

    private boolean isEmail(String value) {
        return value != null && value.trim().contains("@") && !value.trim().contains(" ");
    }

    private String safeMessage(MailException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
