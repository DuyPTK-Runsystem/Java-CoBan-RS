package com.JavaTraining.BaiTap_RS.notification.controller;

import com.JavaTraining.BaiTap_RS.notification.service.NotificationInboxEventService;
import com.JavaTraining.BaiTap_RS.security.UserPrincipal;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v3/notifications")
public class NotificationEventController {

    private final NotificationInboxEventService eventService;

    public NotificationEventController(NotificationInboxEventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter events(@AuthenticationPrincipal UserPrincipal principal) {
        return eventService.subscribe(principal.getId());
    }
}
