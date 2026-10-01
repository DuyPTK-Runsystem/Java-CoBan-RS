package com.JavaTraining.BaiTap_RS.notification;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.JavaTraining.BaiTap_RS.notification.service.NotificationInboxEventService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class NotificationInboxEventServiceTest {

    @Test
    void sendsInboxChangedToConnectedRecipient() throws Exception {
        NotificationInboxEventService service = new NotificationInboxEventService();
        SseEmitter recipientEmitter = Mockito.mock(SseEmitter.class);
        Map<Long, Set<SseEmitter>> registry = new ConcurrentHashMap<>();
        registry.put(10L, ConcurrentHashMap.newKeySet());
        registry.get(10L).add(recipientEmitter);
        ReflectionTestUtils.setField(service, "emittersByUser", registry);

        service.inboxChanged(Set.of(10L));

        Mockito.verify(recipientEmitter).send(Mockito.any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void doesNotSendInboxChangedToOtherConnectedUsers() {
        NotificationInboxEventService service = new NotificationInboxEventService();
        SseEmitter otherEmitter = Mockito.mock(SseEmitter.class);
        Map<Long, Set<SseEmitter>> registry = new ConcurrentHashMap<>();
        registry.put(20L, ConcurrentHashMap.newKeySet());
        registry.get(20L).add(otherEmitter);
        ReflectionTestUtils.setField(service, "emittersByUser", registry);

        service.inboxChanged(Set.of(10L));

        Mockito.verifyNoInteractions(otherEmitter);
    }
}
