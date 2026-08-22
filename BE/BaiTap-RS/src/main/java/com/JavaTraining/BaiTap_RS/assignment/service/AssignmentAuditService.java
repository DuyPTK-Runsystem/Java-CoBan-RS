package com.JavaTraining.BaiTap_RS.assignment.service;

import java.util.LinkedHashMap;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.assignment.domain.entity.HomeroomAssignment;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.audit.domain.entity.AuditLog;
import com.JavaTraining.BaiTap_RS.common.audit.repository.AuditLogRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AssignmentAuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AssignmentAuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public void writeHomeroomAudit(String action, HomeroomAssignment before, HomeroomAssignment after) {
        auditLogRepository.save(new AuditLog(
                AuditContext.currentUserId(),
                action,
                "homeroom_assignment",
                entityId(before, after),
                before == null ? null : auditJson(homeroomData(before)),
                after == null ? null : auditJson(homeroomData(after)),
                AuditContext.requestId(),
                AuditContext.ipAddress()));
    }

    public void writeSubjectTeachingAudit(
            String action,
            SubjectTeachingAssignment before,
            SubjectTeachingAssignment after) {
        auditLogRepository.save(new AuditLog(
                AuditContext.currentUserId(),
                action,
                "subject_teaching_assignment",
                entityId(before, after),
                before == null ? null : auditJson(subjectTeachingData(before)),
                after == null ? null : auditJson(subjectTeachingData(after)),
                AuditContext.requestId(),
                AuditContext.ipAddress()));
    }

    private String entityId(HomeroomAssignment before, HomeroomAssignment after) {
        if (after != null && after.getId() != null) {
            return after.getId().toString();
        }
        return before == null ? "pending" : before.getId().toString();
    }

    private String entityId(SubjectTeachingAssignment before, SubjectTeachingAssignment after) {
        if (after != null && after.getId() != null) {
            return after.getId().toString();
        }
        return before == null ? "pending" : before.getId().toString();
    }

    private Map<String, Object> homeroomData(HomeroomAssignment assignment) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", assignment.getId());
        data.put("classId", assignment.getClassId());
        data.put("teacherId", assignment.getTeacherId());
        data.put("validFrom", assignment.getValidFrom());
        data.put("validTo", assignment.getValidTo());
        data.put("status", assignment.getStatus().name());
        return data;
    }

    private Map<String, Object> subjectTeachingData(SubjectTeachingAssignment assignment) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", assignment.getId());
        data.put("classSubjectId", assignment.getClassSubjectId());
        data.put("teacherId", assignment.getTeacherId());
        data.put("validFrom", assignment.getValidFrom());
        data.put("validTo", assignment.getValidTo());
        data.put("status", assignment.getStatus().name());
        return data;
    }

    private String auditJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException exception) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo dữ liệu audit", exception);
        }
    }
}
