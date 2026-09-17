package com.JavaTraining.BaiTap_RS.notification.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.requests.ReqCreateNotificationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationAudienceType;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationAudienceProjectionRepository;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationAudienceService {

    private final NotificationAudienceCanonicalizer canonicalizer;
    private final NotificationAudienceResolver resolver;

    public NotificationAudienceService(
            UserRepository userRepository,
            SchoolClassRepository schoolClassRepository,
            StudentYearEnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository) {
        this(userRepository, schoolClassRepository, enrollmentRepository, studentRepository, null);
    }

    @Autowired
    public NotificationAudienceService(
            UserRepository userRepository,
            SchoolClassRepository schoolClassRepository,
            StudentYearEnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            NotificationAudienceProjectionRepository projectionRepository) {
        this.canonicalizer = new NotificationAudienceCanonicalizer();
        this.resolver = new NotificationAudienceResolver(
                userRepository,
                schoolClassRepository,
                enrollmentRepository,
                studentRepository,
                projectionRepository,
                canonicalizer);
    }

    public void validateCreateAudience(ReqCreateNotificationDTO request) {
        if (request.getAudienceType() == NotificationAudienceType.INDIVIDUAL) {
            List<Long> userIds = canonicalizer.recipientIds(request.getRecipientUserIds());
            if (resolver.hasProjection()) {
                resolver.validateCreateIndividuals(userIds);
            }
        } else if (request.getAudienceType() == NotificationAudienceType.CLASS
                && resolver.hasProjection()) {
            resolver.validateCreateClass(canonicalizer.parseClassId(request.getTargetReference()));
        }
    }

    public List<Long> canonicalizeRecipientIds(Collection<Long> recipientUserIds) {
        return canonicalizer.recipientIds(recipientUserIds);
    }

    public String canonicalizeClassReference(String targetReference) {
        return canonicalizer.classReference(targetReference);
    }

    public Set<Long> resolveAudienceUserIds(Notification notification, List<Long> explicitRecipientUserIds) {
        return switch (notification.getAudienceType()) {
            case INDIVIDUAL -> resolver.resolveIndividual(notification, explicitRecipientUserIds);
            case CLASS -> resolver.resolveClass(notification);
            case SCHOOL -> resolver.resolveSchoolUsers();
        };
    }
}
