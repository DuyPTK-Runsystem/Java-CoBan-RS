package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.JavaTraining.BaiTap_RS.assignment.domain.entity.HomeroomAssignment;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogWeeklyReview;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;

/** Serializes immutable weekly review state and signed snapshots. */
@Service
@RequiredArgsConstructor
public class LessonLogWeeklySnapshotService {
    private final ObjectMapper objectMapper;

    public String state(LessonLogWeeklyReview review) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("reviewId", review.getId());
        value.put("status", review.getStatus());
        value.put("version", review.getVersion());
        value.put("snapshot", review.getSignedSnapshotJson());
        return json(value);
    }

    public String signedSnapshot(LessonLogWeeklyReview review, List<LessonLogEntry> entries,
            HomeroomAssignment homeroom, Function<LessonLogEntry, Map<String, Object>> entrySnapshot) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("schemaVersion", 1);
        value.put("reviewId", review.getId());
        value.put("classId", review.getClassId());
        value.put("semesterId", review.getSemesterId());
        value.put("weekStart", review.getWeekStart());
        value.put("weekEnd", review.getWeekStart().plusDays(6));
        value.put("status", review.getStatus());
        value.put("version", review.getVersion());
        value.put("weeklyComment", review.getWeeklyComment());
        value.put("weeklyGrade", review.getWeeklyGrade());
        value.put("homeroomAssignment", homeroomSnapshot(homeroom));
        value.put("signedBy", review.getSignedBy());
        value.put("signedAt", review.getSignedAt());
        value.put("entries", entries.stream().map(entrySnapshot).toList());
        return json(value);
    }

    private Map<String, Object> homeroomSnapshot(HomeroomAssignment assignment) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("assignmentId", assignment.getId());
        value.put("classId", assignment.getClassId());
        value.put("teacherId", assignment.getTeacherId());
        value.put("validFrom", assignment.getValidFrom());
        value.put("validTo", assignment.getValidTo());
        value.put("status", assignment.getStatus());
        value.put("assignedBy", assignment.getAssignedBy());
        return value;
    }

    private String json(Object value) {
        try {
            ObjectMapper mapper = objectMapper == null ? new ObjectMapper().registerModule(new JavaTimeModule()) : objectMapper;
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không thể serialize dữ liệu weekly review", exception);
        }
    }
}
