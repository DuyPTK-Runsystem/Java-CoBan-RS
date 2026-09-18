package com.JavaTraining.BaiTap_RS.bootstrap;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationAudienceType;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationChannel;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationDeliveryStatus;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationReceipt;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationStatus;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationReceiptRepository;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(name = "app.seed.demo.enabled", havingValue = "true")
public class DemoNotificationSeeder implements ApplicationRunner {

    private static final String SCHOOL_SCOPE = "DEFAULT_SCHOOL";
    private static final String SENDER_USERNAME = "academic.office";
    private static final String AUDIENCE_INDIVIDUAL = "INDIVIDUAL";
    private static final String AUDIENCE_CLASS = "CLASS";
    private static final String AUDIENCE_SCHOOL = "SCHOOL";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_SCHEDULED = "SCHEDULED";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String CHANNEL_IN_APP = "IN_APP";
    private static final String CHANNEL_EMAIL = "EMAIL";

    private final NotificationRepository notificationRepository;
    private final NotificationReceiptRepository receiptRepository;
    private final UserRepository userRepository;

    public DemoNotificationSeeder(
            NotificationRepository notificationRepository,
            NotificationReceiptRepository receiptRepository,
            UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.receiptRepository = receiptRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Optional<User> sender = userRepository.findByUsername(SENDER_USERNAME);
        if (sender.isEmpty()) {
            return;
        }

        for (NotificationSpec spec : specs()) {
            seedNotification(spec, sender.get().getId());
        }
    }

    private void seedNotification(NotificationSpec spec, Long senderId) {
        Optional<NotificationAudienceType> audience = enumValue(
                NotificationAudienceType.class, spec.audienceType());
        Optional<NotificationStatus> status = enumValue(NotificationStatus.class, spec.status());
        Optional<NotificationChannel> channel = enumValue(NotificationChannel.class, spec.channel());
        if (audience.isEmpty() || status.isEmpty() || channel.isEmpty()) {
            return;
        }

        Notification notification = notificationRepository.findByIdempotencyKey(spec.key())
                .orElseGet(() -> notificationRepository.save(createNotification(spec, senderId,
                        audience.get(), status.get(), channel.get())));

        for (ReceiptSpec receiptSpec : spec.receipts()) {
            Optional<User> recipient = findFirstExisting(receiptSpec.usernames());
            if (recipient.isEmpty()) {
                continue;
            }
            if (receiptRepository.existsByNotificationIdAndRecipientUserId(
                    notification.getId(), recipient.get().getId())) {
                continue;
            }

            receiptRepository.save(createReceipt(notification, recipient.get(), receiptSpec));
        }
    }

    private NotificationReceipt createReceipt(
            Notification notification,
            User recipient,
            ReceiptSpec receiptSpec) {
        NotificationReceipt receipt = new NotificationReceipt(
                notification.getId(), recipient.getId(), receiptSpec.accessScope());
        receipt.setReadAt(receiptSpec.readAt());
        receipt.setDeliveryStatus(receiptSpec.deliveryStatus());
        receipt.setDeliveryError(receiptSpec.deliveryError());
        receipt.setDeliveredAt(receiptSpec.deliveredAt());
        return receipt;
    }

    private Notification createNotification(
            NotificationSpec spec,
            Long senderId,
            NotificationAudienceType audience,
            NotificationStatus status,
            NotificationChannel channel) {
        Notification notification = new Notification(
                spec.title(), spec.body(), audience, spec.targetReference(), senderId, SCHOOL_SCOPE);
        notification.setStatus(status);
        notification.setChannel(channel);
        notification.setPublishAt(spec.publishAt());
        notification.setExpiresAt(spec.expiresAt());
        notification.setIdempotencyKey(spec.key());
        notification.setIdempotencyFingerprint(spec.key());
        return notification;
    }

    private Optional<User> findFirstExisting(List<String> usernames) {
        for (String username : usernames) {
            Optional<User> user = userRepository.findByUsername(username);
            if (user.isPresent()) {
                return user;
            }
        }
        return Optional.empty();
    }

    private static <T extends Enum<T>> Optional<T> enumValue(Class<T> enumType, String value) {
        try {
            return Optional.of(Enum.valueOf(enumType, value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static List<NotificationSpec> specs() {
        LocalDateTime publishedAt = LocalDateTime.of(2026, 9, 18, 8, 0);
        return List.of(
                new NotificationSpec(
                        "NOTI-081-IND-01", "Cập nhật hồ sơ", "Vui lòng cập nhật hồ sơ cá nhân.",
                        AUDIENCE_INDIVIDUAL, STATUS_PUBLISHED, CHANNEL_IN_APP, "teacher01", publishedAt, null,
                        List.of(new ReceiptSpec(
                                List.of("pham.minh.quan", "teacher01"), AUDIENCE_INDIVIDUAL,
                                LocalDateTime.of(2026, 9, 18, 8, 30), null, null, null))),
                new NotificationSpec(
                        "NOTI-081-IND-02", "Kết quả học tập", "Kết quả học tập đã được cập nhật.",
                        AUDIENCE_INDIVIDUAL, STATUS_PUBLISHED, CHANNEL_IN_APP, "student.6a1.01", publishedAt, null,
                        List.of(new ReceiptSpec(
                                List.of("nguyen.minh.khang61", "student.6a1.01"), AUDIENCE_INDIVIDUAL,
                                null, null, null, null))),
                new NotificationSpec(
                        "NOTI-081-CLASS-01", "Lịch học tuần này", "Lịch học lớp 6A1 tuần này.",
                        AUDIENCE_CLASS, STATUS_PUBLISHED, CHANNEL_IN_APP, "6A1", publishedAt,
                        LocalDateTime.of(2026, 9, 25, 23, 59),
                        List.of(
                                new ReceiptSpec(List.of("nguyen.minh.khang61", "student.6a1.01"),
                                        AUDIENCE_CLASS, null, null, null, null),
                                new ReceiptSpec(List.of("nguyen.ngoc.anh61", "student.6a1.02"),
                                        AUDIENCE_CLASS, LocalDateTime.of(2026, 9, 18, 9, 0), null, null, null))),
                new NotificationSpec(
                        "NOTI-081-SCHOOL-01", "Thông báo toàn trường", "Thông báo dành cho toàn trường.",
                        AUDIENCE_SCHOOL, STATUS_SCHEDULED, CHANNEL_IN_APP, null,
                        LocalDateTime.of(2026, 9, 20, 8, 0), LocalDateTime.of(2026, 9, 30, 23, 59),
                        List.of()),
                new NotificationSpec(
                        "NOTI-081-EMAIL-01", "Nhắc lịch họp", "Nhắc lịch họp dành cho giáo viên.",
                        AUDIENCE_INDIVIDUAL, STATUS_PUBLISHED, CHANNEL_EMAIL, "teacher03", publishedAt, null,
                        List.of(
                                new ReceiptSpec(List.of("tran.thu.ha", "teacher03"), AUDIENCE_INDIVIDUAL,
                                        null, NotificationDeliveryStatus.SENT, null, publishedAt),
                                new ReceiptSpec(List.of("le.hoang.nam", "teacher04"), AUDIENCE_INDIVIDUAL,
                                        null, NotificationDeliveryStatus.FAILED, "SMTP demo failure", null))),
                new NotificationSpec(
                        "NOTI-081-EXPIRED", "Hạn đăng ký đã kết thúc", "Hạn đăng ký lớp 6A2 đã kết thúc.",
                        AUDIENCE_CLASS, STATUS_EXPIRED, CHANNEL_IN_APP, "6A2",
                        LocalDateTime.of(2026, 9, 10, 8, 0), LocalDateTime.of(2026, 9, 17, 23, 59),
                        List.of()),
                new NotificationSpec(
                        "NOTI-081-CANCELLED", "Lịch cũ đã huỷ", "Lịch cũ đã được huỷ.",
                        AUDIENCE_SCHOOL, STATUS_CANCELLED, CHANNEL_IN_APP, null, null, null, List.of()),
                new NotificationSpec(
                        "NOTI-081-DRAFT", "Bản nháp học vụ", "Bản nháp học vụ chưa phát hành.",
                        AUDIENCE_INDIVIDUAL, STATUS_DRAFT, CHANNEL_IN_APP, SENDER_USERNAME, null, null, List.of()));
    }

    private record NotificationSpec(
            String key,
            String title,
            String body,
            String audienceType,
            String status,
            String channel,
            String targetReference,
            LocalDateTime publishAt,
            LocalDateTime expiresAt,
            List<ReceiptSpec> receipts) {
    }

    private record ReceiptSpec(
            List<String> usernames,
            String accessScope,
            LocalDateTime readAt,
            NotificationDeliveryStatus deliveryStatus,
            String deliveryError,
            LocalDateTime deliveredAt) {
    }
}
