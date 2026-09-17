package com.JavaTraining.BaiTap_RS.notification.service;

import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqCreateNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqPublishNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationReceiptDTO;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationIndividualAudienceProjectionRepository;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationReceiptRepository;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationRepository;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationServiceOperations operations;

    public NotificationService(NotificationRepository repo, NotificationReceiptRepository receiptRepo,
            NotificationAudienceService audienceService, NotificationAuditService auditService) {
        this(repo, receiptRepo, audienceService, auditService, null, null, null);
    }

    public NotificationService(NotificationRepository repo, NotificationReceiptRepository receiptRepo,
            NotificationAudienceService audienceService, NotificationAuditService auditService,
            SchoolClassRepository schoolClassRepository,
            NotificationIndividualAudienceProjectionRepository projection) {
        this(repo, receiptRepo, audienceService, auditService, schoolClassRepository, projection, null);
    }

    public NotificationService(NotificationRepository repo, NotificationReceiptRepository receiptRepo,
            NotificationAudienceService audienceService, NotificationAuditService auditService,
            SchoolClassRepository schoolClassRepository,
            NotificationIndividualAudienceProjectionRepository projection,
            TeacherRepository teacherRepository, UserRepository userRepository,
            JavaMailSender mailSender, String fromEmail) {
        this(repo, receiptRepo, audienceService, auditService, schoolClassRepository, projection,
                new NotificationEmailDeliveryService(teacherRepository, userRepository, mailSender, fromEmail));
    }

    @Autowired
    public NotificationService(NotificationRepository repo, NotificationReceiptRepository receiptRepo,
            NotificationAudienceService audienceService, NotificationAuditService auditService,
            SchoolClassRepository schoolClassRepository,
            NotificationIndividualAudienceProjectionRepository projection,
            NotificationEmailDeliveryService emailDeliveryService) {
        this.operations = new NotificationServiceOperations(new NotificationServiceWiring(
                repo, receiptRepo, audienceService, auditService, schoolClassRepository, projection,
                emailDeliveryService));
    }

    @Transactional
    public ResNotificationDTO createDraft(ReqCreateNotificationDTO request, Long actorUserId) {
        return operations.createDraft(request, actorUserId);
    }

    @Transactional
    public ResNotificationDTO publish(Long id, ReqPublishNotificationDTO request, Long actorUserId) {
        return publish(id, request, actorUserId, true);
    }

    @Transactional
    public ResNotificationDTO publish(Long id, ReqPublishNotificationDTO request, Long actorUserId,
            boolean canManageAll) {
        return operations.publish(id, request, actorUserId, canManageAll);
    }

    @Transactional
    public ResNotificationDTO cancel(Long id, Long actorUserId) {
        return cancel(id, actorUserId, true);
    }

    @Transactional
    public ResNotificationDTO cancel(Long id, Long actorUserId, boolean canManageAll) {
        return operations.cancel(id, actorUserId, canManageAll);
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO<ResNotificationDTO> getInbox(Long actorUserId, Boolean unreadOnly, Pageable pageable) {
        return operations.getInbox(actorUserId, unreadOnly, pageable);
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO<ResNotificationDTO> getManagedNotifications(Long actorUserId, Pageable pageable) {
        return getManagedNotifications(actorUserId, pageable, true);
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO<ResNotificationDTO> getManagedNotifications(Long actorUserId, Pageable pageable,
            boolean canManageAll) {
        return operations.getManagedNotifications(actorUserId, pageable, canManageAll);
    }

    @Transactional(readOnly = true)
    public ResNotificationDTO getNotificationDetail(Long id, Long actorUserId, boolean isManager) {
        return operations.getNotificationDetail(id, actorUserId, isManager);
    }

    @Transactional
    public ResNotificationReceiptDTO markAsRead(Long id, Long actorUserId) {
        return operations.markAsRead(id, actorUserId);
    }
}
