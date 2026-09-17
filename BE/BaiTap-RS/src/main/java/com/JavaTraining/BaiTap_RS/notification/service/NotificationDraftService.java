package com.JavaTraining.BaiTap_RS.notification.service;

import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqCreateNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationAudienceType;

public class NotificationDraftService {

    private final NotificationAuditService notificationAuditService;
    private final NotificationRequestValidator requestValidator;
    private final NotificationIdempotencyService idempotencyService;
    private final NotificationResponseMapper responseMapper;
    private final NotificationAudienceService audienceService;

    public NotificationDraftService(
            NotificationAuditService notificationAuditService,
            NotificationRequestValidator requestValidator,
            NotificationIdempotencyService idempotencyService,
            NotificationResponseMapper responseMapper,
            NotificationAudienceService audienceService) {
        this.notificationAuditService = notificationAuditService;
        this.requestValidator = requestValidator;
        this.idempotencyService = idempotencyService;
        this.responseMapper = responseMapper;
        this.audienceService = audienceService;
    }

    public ResNotificationDTO createDraft(ReqCreateNotificationDTO request, Long actorUserId) {
        requestValidator.validateCreateRequest(request);
        audienceService.validateCreateAudience(request);
        String fingerprint = idempotencyService.buildFingerprint(request);
        Notification existing = idempotencyService.findExisting(
                request.getIdempotencyKey(), fingerprint, actorUserId);
        if (existing != null) {
            return responseMapper.toResponse(existing, null, null, true);
        }

        Notification notification = new Notification(
                request.getTitle(),
                request.getBody(),
                request.getAudienceType(),
                resolveTargetReference(request),
                actorUserId,
                request.getSchoolScope());
        notification.setChannel(request.getChannel() == null
                ? com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationChannel.IN_APP
                : request.getChannel());
        applyOptionalFields(notification, request, fingerprint);

        Notification saved = idempotencyService.save(notification);
        notificationAuditService.auditCreateDraft(actorUserId, saved);
        return responseMapper.toResponse(saved, null, null, true);
    }

    private String resolveTargetReference(ReqCreateNotificationDTO request) {
        if (request.getAudienceType() == NotificationAudienceType.INDIVIDUAL
                && request.getRecipientUserIds() != null
                && !request.getRecipientUserIds().isEmpty()) {
            return audienceService.canonicalizeRecipientIds(request.getRecipientUserIds()).stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
        }
        if (request.getAudienceType() == NotificationAudienceType.CLASS) {
            return audienceService.canonicalizeClassReference(request.getTargetReference());
        }
        return request.getTargetReference();
    }

    private void applyOptionalFields(
            Notification notification, ReqCreateNotificationDTO request, String fingerprint) {
        if (request.getExpiresAt() != null) {
            notification.setExpiresAt(request.getExpiresAt());
        }
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            notification.setIdempotencyKey(request.getIdempotencyKey());
            notification.setIdempotencyFingerprint(fingerprint);
        }
    }
}
