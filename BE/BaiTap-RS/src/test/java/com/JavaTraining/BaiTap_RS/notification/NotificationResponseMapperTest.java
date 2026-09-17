package com.JavaTraining.BaiTap_RS.notification;

import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationAudienceType;
import com.JavaTraining.BaiTap_RS.notification.service.NotificationResponseMapper;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class NotificationResponseMapperTest {

    @Test
    void mapsLegacyNotificationWithoutEmbeddedIdempotency() {
        Notification notification = new Notification(
                "Tiêu đề",
                "Nội dung",
                NotificationAudienceType.CLASS,
                "101",
                2L,
                "DEFAULT_SCHOOL");
        notification.setIdempotency(null);

        var response = new NotificationResponseMapper().toResponse(notification, null, null, true);

        assertNull(response.getIdempotencyKey());
        assertNull(response.getTargetReference());
        assertNull(response.getAudienceDetails());
    }
}
