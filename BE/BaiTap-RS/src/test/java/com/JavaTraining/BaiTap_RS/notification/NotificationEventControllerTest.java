package com.JavaTraining.BaiTap_RS.notification;

import com.JavaTraining.BaiTap_RS.notification.controller.NotificationEventController;
import com.JavaTraining.BaiTap_RS.notification.service.NotificationInboxEventService;
import com.JavaTraining.BaiTap_RS.security.UserPrincipal;
import com.JavaTraining.BaiTap_RS.user.domain.entity.Role;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class NotificationEventControllerTest {

    @Test
    void subscribesAuthenticatedPrincipalToItsOwnEventStream() {
        NotificationInboxEventService eventService = Mockito.mock(NotificationInboxEventService.class);
        NotificationEventController controller = new NotificationEventController(eventService);
        SseEmitter expected = new SseEmitter();
        Mockito.when(eventService.subscribe(23L)).thenReturn(expected);

        SseEmitter actual = controller.events(principal());

        Assertions.assertSame(expected, actual, "The endpoint returns the authenticated user's stream");
    }

    @Test
    void declaresServerSentEventMediaType() throws NoSuchMethodException {
        GetMapping mapping = NotificationEventController.class.getMethod("events", UserPrincipal.class)
                .getAnnotation(GetMapping.class);
        Assertions.assertEquals(MediaType.TEXT_EVENT_STREAM_VALUE, mapping.produces()[0],
                "The endpoint declares SSE media type");
    }

    @Test
    void requiresAuthentication() throws NoSuchMethodException {
        PreAuthorize authorization = NotificationEventController.class.getMethod("events", UserPrincipal.class)
                .getAnnotation(PreAuthorize.class);
        Assertions.assertEquals("isAuthenticated()", authorization.value(), "The endpoint requires authentication");
    }

    private UserPrincipal principal() {
        User user = new User("student", "password");
        ReflectionTestUtils.setField(user, "id", 23L);
        user.addRole(new Role("STUDENT", "Student", "Student"));
        return new UserPrincipal(user);
    }
}
