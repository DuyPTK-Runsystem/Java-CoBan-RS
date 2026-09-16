package com.JavaTraining.BaiTap_RS.notification;

import static org.junit.jupiter.api.Assertions.assertNull;

import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationAudienceType;
import com.JavaTraining.BaiTap_RS.notification.service.NotificationResponseMapper;
import org.junit.jupiter.api.Test;

class NotificationResponseMapperTest {

    @Test
    void mapsLegacyNotificationWithoutEmbeddedIdempotency() {
        Notification notification = new Notification(
                "Tiêu đề",
                "Nội dung",
                NotificationAudienceType.SCHOOL,
                null,
                2L,
                "DEFAULT_SCHOOL");
        notification.setIdempotency(null);

        var response = new NotificationResponseMapper().toResponse(notification, null, null, true);

        assertNull(response.getIdempotencyKey());
    }
}
