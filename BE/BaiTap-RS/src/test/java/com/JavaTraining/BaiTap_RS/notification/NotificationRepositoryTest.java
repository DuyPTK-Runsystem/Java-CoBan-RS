package com.JavaTraining.BaiTap_RS.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationAudienceType;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notification-repository;MODE=MySQL;DATABASE_TO_UPPER=false;"
                + "NON_KEYWORDS=USER,ROLE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void findsNotificationByEmbeddedIdempotencyKey() {
        Notification notification = new Notification(
                "Thông báo kiểm tra", "Nội dung kiểm tra", NotificationAudienceType.SCHOOL,
                null, 1L, "DEFAULT_SCHOOL");
        notification.setIdempotencyKey("notification-key-1");
        Notification saved = notificationRepository.saveAndFlush(notification);

        Notification found = notificationRepository.findByIdempotencyKey("notification-key-1").orElseThrow();

        assertEquals(saved.getId(), found.getId(), "embedded idempotency key must resolve its notification");
        assertTrue(notificationRepository.findByIdempotencyKey("missing-key").isEmpty(),
                "an unused idempotency key must not resolve a notification");
    }
}
