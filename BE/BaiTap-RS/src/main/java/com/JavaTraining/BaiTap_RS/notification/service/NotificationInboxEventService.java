package com.JavaTraining.BaiTap_RS.notification.service;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class NotificationInboxEventService {

    public static final String INBOX_CHANGED_EVENT = "inbox-changed";

    private final ConcurrentMap<Long, Set<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        emittersByUser.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(emitter);
        Runnable cleanup = () -> remove(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());
        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException exception) {
            cleanup.run();
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    public void inboxChanged(Set<Long> recipientUserIds) {
        for (Long userId : recipientUserIds) {
            Set<SseEmitter> emitters = emittersByUser.get(userId);
            if (emitters == null) {
                continue;
            }
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name(INBOX_CHANGED_EVENT));
                } catch (IOException | IllegalStateException exception) {
                    emitter.completeWithError(exception);
                    remove(userId, emitter);
                }
            }
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        Set<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emittersByUser.remove(userId, emitters);
            }
        }
    }
}
