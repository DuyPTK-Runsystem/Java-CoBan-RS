package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogRevision;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogRevisionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LessonLogAuditService {
    private final LessonLogRevisionRepository revisions;
    private final ObjectMapper objectMapper;

    public void record(LessonLogEntry entry, String action, String reason) {
        record(entry, new LessonLogAuditChange(action, reason, null, state(entry)));
    }

    public void record(LessonLogEntry entry, LessonLogAuditChange change) {
        revisions.save(new LessonLogRevision(entry.getId(), null, null, change.action(), AuditContext.currentUserId(),
                change.reason(), change.beforeState(), change.afterState()));
    }

    public String state(LessonLogEntry entry) { return serialize(snapshot(entry)); }

    public Map<String, Object> snapshot(LessonLogEntry entry) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("entryId", entry.getId()); value.put("timetableEntryId", entry.getTimetableEntryId());
        value.put("timetableRevisionId", entry.getTimetableRevisionId()); value.put("assignmentId", entry.getAssignmentId());
        value.put("semesterId", entry.getSemesterId()); value.put("classId", entry.getClassId());
        value.put("subjectId", entry.getSubjectId()); value.put("assignedTeacherId", entry.getAssignedTeacherId());
        value.put("policyId", entry.getPolicyId()); value.put("lessonDate", entry.getLessonDate());
        value.put("session", entry.getSession()); value.put("periodIndex", entry.getPeriodIndex());
        value.put("lessonEndsAt", entry.getLessonEndsAt()); value.put("editWindowExpiresAt", entry.getEditWindowExpiresAt());
        value.put("sourceSnapshotJson", entry.getSourceSnapshotJson()); value.put("rosterCountSnapshot", entry.getRosterCountSnapshot());
        value.put("status", entry.getStatus()); value.put("version", entry.getVersion()); value.put("title", entry.getTitle());
        value.put("content", entry.getContent()); value.put("completionStatus", entry.getCompletionStatus());
        value.put("grade", entry.getGrade()); value.put("presentCount", entry.getPresentCount());
        value.put("absentCount", entry.getAbsentCount()); value.put("comments", entry.getComments());
        value.put("absentStudentNotes", entry.getAbsentStudentNotes()); value.put("homework", entry.getHomework());
        value.put("submittedAt", entry.getSubmittedAt()); value.put("submittedBy", entry.getSubmittedBy());
        value.put("reviewedAt", entry.getReviewedAt()); value.put("reviewedBy", entry.getReviewedBy());
        value.put("reviewComment", entry.getReviewComment()); return value;
    }

    private String serialize(Object value) {
        try { return (objectMapper == null ? new ObjectMapper().registerModule(new JavaTimeModule()) : objectMapper)
                .writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Không thể serialize dữ liệu audit JSON", exception); }
    }
}
