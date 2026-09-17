package com.JavaTraining.BaiTap_RS.notification.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationClassAudienceDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationAudienceClassContextDTO;
import com.JavaTraining.BaiTap_RS.notification.domain.DTOs.response.ResNotificationIndividualAudienceDTO;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationAudienceProjectionRepository;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationAudienceProjectionRepository.ClassAudienceProjection;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationIndividualAudienceProjectionRepository;
import com.JavaTraining.BaiTap_RS.notification.repository.NotificationIndividualAudienceProjectionRepository.IndividualAudienceProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationAudienceLookupService {

    private static final String DEFAULT_SCHOOL_SCOPE = "DEFAULT_SCHOOL";

    private final NotificationAudienceProjectionRepository audienceRepository;
    private final NotificationIndividualAudienceProjectionRepository individualRepository;

    public NotificationAudienceLookupService(
            NotificationAudienceProjectionRepository audienceRepository,
            NotificationIndividualAudienceProjectionRepository individualRepository) {
        this.audienceRepository = audienceRepository;
        this.individualRepository = individualRepository;
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO<ResNotificationClassAudienceDTO> findClasses(String query, Pageable pageable) {
        Page<ClassAudienceProjection> page = audienceRepository.findEligibleClasses(normalizeQuery(query), pageable);
        return new ResultPaginationDTO<>(
                toMeta(page),
                page.getContent().stream()
                        .map(item -> new ResNotificationClassAudienceDTO(
                                item.getClassId(),
                                item.getClassCode(),
                                item.getClassName(),
                                item.getAcademicYearId(),
                                item.getEligibleRecipientCount()))
                        .toList());
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO<ResNotificationIndividualAudienceDTO> findIndividuals(
            String query, String roleCode, Long studentClassId, Long teacherClassId, Pageable pageable) {
        Page<IndividualAudienceProjection> page = individualRepository.findEligibleIndividuals(
                normalizeQuery(query), roleCode, studentClassId, teacherClassId, pageable);
        List<Long> userIds = page.getContent().stream().map(IndividualAudienceProjection::getUserId).toList();
        Map<Long, List<String>> roles = new HashMap<>();
        Map<Long, ResNotificationAudienceClassContextDTO> studentClasses = new HashMap<>();
        Map<Long, List<ResNotificationAudienceClassContextDTO>> teacherClasses = new HashMap<>();
        if (!userIds.isEmpty()) {
            individualRepository.findRoleCodesByUserIds(userIds).forEach(item ->
                    roles.computeIfAbsent(item.getUserId(), ignored -> new ArrayList<>()).add(item.getRoleCode()));
            individualRepository.findStudentClassContextsByUserIds(userIds).forEach(item ->
                    studentClasses.putIfAbsent(item.getUserId(), toContext(item)));
            addTeacherContexts(teacherClasses, individualRepository.findTeacherHomeroomContextsByUserIds(userIds));
            addTeacherContexts(teacherClasses, individualRepository.findTeacherSubjectContextsByUserIds(userIds));
        }
        return new ResultPaginationDTO<>(
                toMeta(page),
                page.getContent().stream()
                        .map(item -> new ResNotificationIndividualAudienceDTO(
                                item.getUserId(),
                                item.getDisplayName(),
                                item.getStudentCode(),
                                item.getUsername(),
                                roles.getOrDefault(item.getUserId(), List.of()),
                                studentClasses.get(item.getUserId()),
                                teacherClasses.getOrDefault(item.getUserId(), List.of())))
                        .toList());
    }

    private ResNotificationAudienceClassContextDTO toContext(
            NotificationIndividualAudienceProjectionRepository.ClassContextProjection item) {
        return new ResNotificationAudienceClassContextDTO(item.getClassId(), item.getClassCode(), item.getClassName());
    }

    private void addTeacherContexts(
            Map<Long, List<ResNotificationAudienceClassContextDTO>> contexts,
            List<NotificationIndividualAudienceProjectionRepository.ClassContextProjection> projections) {
        projections.forEach(item -> contexts.computeIfAbsent(item.getUserId(), ignored -> new ArrayList<>())
                .add(toContext(item)));
        contexts.replaceAll((userId, items) -> new ArrayList<>(new LinkedHashSet<>(items)));
        contexts.values().forEach(items -> items.sort(java.util.Comparator.comparing(
                ResNotificationAudienceClassContextDTO::classId)));
    }

    public String defaultSchoolScope() {
        return DEFAULT_SCHOOL_SCOPE;
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String normalized = query.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private ResultPaginationDTO.Meta toMeta(Page<?> page) {
        return new ResultPaginationDTO.Meta(
                page.getNumber(), page.getSize(), page.getTotalPages(), page.getTotalElements());
    }
}
