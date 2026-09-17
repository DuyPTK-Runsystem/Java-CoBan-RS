package com.JavaTraining.BaiTap_RS.notification.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationAudienceDetailsDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.Notification;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationAudienceType;
import com.JavaTraining.BaiTap_RS.notification.domain.entity.NotificationReceipt;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationIndividualAudienceProjectionRepository;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationIndividualAudienceProjectionRepository.IndividualDisplayNameProjection;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationReceiptRepository;

/**
 * Resolves manager-only notification audience labels without returning internal
 * user or class identifiers to the API client.
 */
public class NotificationAudienceDetailService {

    private static final String SCHOOL_LABEL = "Toàn trường";
    private static final String CLASS_LABEL = "Lớp học đã chọn";
    private static final String INDIVIDUAL_LABEL = "Người nhận cụ thể";

    private final NotificationReceiptRepository receiptRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final NotificationIndividualAudienceProjectionRepository individualRepository;
    private final NotificationAudienceCanonicalizer canonicalizer = new NotificationAudienceCanonicalizer();

    public NotificationAudienceDetailService(
            NotificationReceiptRepository receiptRepository,
            SchoolClassRepository schoolClassRepository,
            NotificationIndividualAudienceProjectionRepository individualRepository) {
        this.receiptRepository = receiptRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.individualRepository = individualRepository;
    }

    public ResNotificationAudienceDetailsDTO resolve(Notification notification) {
        List<NotificationReceipt> receipts = receiptRepository.findByNotificationId(notification.getId());
        int recipientCount = distinctRecipientCount(receipts);
        return switch (notification.getAudienceType()) {
            case SCHOOL -> schoolDetails(recipientCount);
            case CLASS -> classDetails(notification, recipientCount);
            case INDIVIDUAL -> individualDetails(notification, receipts);
        };
    }

    private ResNotificationAudienceDetailsDTO schoolDetails(int receiptCount) {
        return new ResNotificationAudienceDetailsDTO(
                NotificationAudienceType.SCHOOL,
                SCHOOL_LABEL,
                receiptCount == 0 ? null : receiptCount,
                List.of(),
                receiptCount > 0);
    }

    private ResNotificationAudienceDetailsDTO classDetails(Notification notification, int receiptCount) {
        SchoolClass schoolClass = findClass(notification.getTargetReference());
        String displayLabel = schoolClass == null ? CLASS_LABEL : classDisplayLabel(schoolClass);
        return new ResNotificationAudienceDetailsDTO(
                NotificationAudienceType.CLASS,
                displayLabel,
                receiptCount == 0 ? null : receiptCount,
                List.of(),
                schoolClass != null);
    }

    private ResNotificationAudienceDetailsDTO individualDetails(
            Notification notification, List<NotificationReceipt> receipts) {
        Set<Long> userIds = receipts.stream()
                .map(NotificationReceipt::getRecipientUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (userIds.isEmpty()) {
            userIds.addAll(parseIndividualReference(notification.getTargetReference()));
        }

        List<String> displayNames = findDisplayNames(userIds);
        Integer recipientCount = userIds.isEmpty() ? null : userIds.size();
        boolean displayDataAvailable = recipientCount != null && displayNames.size() == recipientCount;
        return new ResNotificationAudienceDetailsDTO(
                NotificationAudienceType.INDIVIDUAL,
                INDIVIDUAL_LABEL,
                recipientCount,
                displayNames,
                displayDataAvailable);
    }

    private SchoolClass findClass(String targetReference) {
        try {
            return schoolClassRepository.findById(canonicalizer.parseClassId(targetReference)).orElse(null);
        } catch (AppException exception) {
            return null;
        }
    }

    private Set<Long> parseIndividualReference(String targetReference) {
        try {
            return canonicalizer.legacyIndividualReference(targetReference);
        } catch (AppException exception) {
            return Set.of();
        }
    }

    private List<String> findDisplayNames(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return individualRepository.findDisplayNamesByUserIds(userIds).stream()
                .map(IndividualDisplayNameProjection::getDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .toList();
    }

    private int distinctRecipientCount(List<NotificationReceipt> receipts) {
        return receipts.stream()
                .map(NotificationReceipt::getRecipientUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .size();
    }

    private String classDisplayLabel(SchoolClass schoolClass) {
        if (schoolClass.getClassName() != null && !schoolClass.getClassName().isBlank()) {
            return schoolClass.getClassName();
        }
        if (schoolClass.getClassCode() != null && !schoolClass.getClassCode().isBlank()) {
            return schoolClass.getClassCode();
        }
        return CLASS_LABEL;
    }
}
