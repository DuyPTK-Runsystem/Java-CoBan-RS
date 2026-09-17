package com.JavaTraining.BaiTap_RS.notification.service;

import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationIndividualAudienceProjectionRepository;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationReceiptRepository;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationRepository;

public final class NotificationServiceWiring {
    private final NotificationDraftService draftServiceComponent;
    private final NotificationLifecycleService lifecycleServiceComponent;
    private final NotificationQueryService queryServiceComponent;
    private final NotificationReadService readServiceComponent;

    public NotificationServiceWiring(NotificationRepository notificationRepository,
            NotificationReceiptRepository notificationReceiptRepository,
            NotificationAudienceService notificationAudienceService,
            NotificationAuditService notificationAuditService,
            SchoolClassRepository schoolClassRepository,
            NotificationIndividualAudienceProjectionRepository individualAudienceProjectionRepository,
            NotificationEmailDeliveryService emailDeliveryService) {
        NotificationResponseMapper responseMapper = new NotificationResponseMapper();
        NotificationAudienceDetailService audienceDetailService = schoolClassRepository == null
                || individualAudienceProjectionRepository == null ? null
                : new NotificationAudienceDetailService(notificationReceiptRepository, schoolClassRepository,
                        individualAudienceProjectionRepository);
        NotificationRequestValidator requestValidator = new NotificationRequestValidator();
        NotificationIdempotencyService idempotencyService = new NotificationIdempotencyService(notificationRepository);
        this.draftServiceComponent = new NotificationDraftService(notificationAuditService, requestValidator,
                idempotencyService, responseMapper, notificationAudienceService);
        this.lifecycleServiceComponent = new NotificationLifecycleService(notificationRepository, notificationReceiptRepository,
                notificationAudienceService, notificationAuditService, responseMapper, requestValidator,
                emailDeliveryService);
        this.queryServiceComponent = new NotificationQueryService(notificationRepository, notificationReceiptRepository,
                responseMapper, audienceDetailService);
        this.readServiceComponent = new NotificationReadService(notificationReceiptRepository, notificationRepository,
                notificationAuditService);
    }

    public NotificationDraftService draftService() {
        return draftServiceComponent;
    }

    public NotificationLifecycleService lifecycleService() {
        return lifecycleServiceComponent;
    }

    public NotificationQueryService queryService() {
        return queryServiceComponent;
    }

    public NotificationReadService readService() {
        return readServiceComponent;
    }
}
