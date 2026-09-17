package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogEntryResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogPolicy;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LessonLogResponseMappingService {
    private final LessonLogEntryEditor entryEditor;
    private final TeacherRepository teachers;
    private final LessonLogResponseLookupService lookupService;
    private final ObjectMapper objectMapper;

    public LessonLogEntryResponse map(LessonLogEntry entry) {
        boolean canEdit = LessonLogEntryAuthorization.canEdit(entry, teachers);
        boolean canReview = LessonLogEntryAuthorization.canReview(entry);
        boolean canAmend = LessonLogEntryAuthorization.canAmend(entry);
        LessonLogEntryResponseMapper.ResponsePermissions permissions = new LessonLogEntryResponseMapper.ResponsePermissions(
                canEdit, canEdit && entryEditor.hasRequiredFields(entry), canReview, canAmend,
                canEdit || canReview || canAmend ? null : LessonLogEntryAuthorization.blockedReason(entry, teachers),
                rubricFor(entry));
        return LessonLogEntryResponseMapper.map(entry, lookupService.displayValues(entry), permissions);
    }

    public List<Object> rubric(LessonLogPolicy policy) {
        if (policy == null || policy.getRubricJson() == null || policy.getRubricJson().isBlank()) {
            return List.of();
        }
        try {
            com.fasterxml.jackson.databind.JsonNode node = (objectMapper == null ? new ObjectMapper() : objectMapper)
                    .readTree(policy.getRubricJson());
            if (!node.isArray()) {
                return List.of();
            }
            List<Object> values = new ArrayList<>();
            node.forEach(values::add);
            return values;
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private List<Object> rubricFor(LessonLogEntry entry) {
        return rubric(lookupService.policy(entry.getPolicyId()));
    }
}
