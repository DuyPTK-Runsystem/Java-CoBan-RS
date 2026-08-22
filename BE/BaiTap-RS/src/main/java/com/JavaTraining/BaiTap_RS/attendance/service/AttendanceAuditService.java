package com.JavaTraining.BaiTap_RS.attendance.service;

import java.util.LinkedHashMap;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceRecord;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSession;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.audit.domain.entity.AuditLog;
import com.JavaTraining.BaiTap_RS.common.audit.repository.AuditLogRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AttendanceAuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AttendanceAuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public void writeRecordAudit(
            String action,
            AttendanceSession session,
            Map<String, Object> beforeData,
            AttendanceRecord after) {
        auditLogRepository.save(new AuditLog(
                AuditContext.currentUserId(),
                action,
                "attendance_record",
                entityId(beforeData, after),
                beforeData == null ? null : auditJson(beforeData),
                after == null ? null : auditJson(recordData(session, after)),
                AuditContext.requestId(),
                AuditContext.ipAddress()));
    }

    public Map<String, Object> recordData(AttendanceSession session, AttendanceRecord record) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("attendanceRecordId", record.getId());
        data.put("sessionId", record.getSessionId());
        data.put("classId", session.getClassId());
        data.put("semesterId", session.getSemesterId());
        data.put("attendanceDate", session.getAttendanceDate());
        data.put("sessionPeriod", session.getSessionPeriod().name());
        data.put("studentId", record.getStudentId());
        data.put("status", record.getStatus().name());
        data.put("note", record.getNote());
        data.put("recordedBy", record.getRecordedBy());
        data.put("recordedAt", record.getRecordedAt());
        data.put("updatedBy", record.getUpdatedBy());
        data.put("updatedAt", record.getUpdatedAt());
        return data;
    }

    private String entityId(Map<String, Object> beforeData, AttendanceRecord after) {
        if (after != null && after.getId() != null) {
            return after.getId().toString();
        }
        if (beforeData != null && beforeData.get("attendanceRecordId") != null) {
            return beforeData.get("attendanceRecordId").toString();
        }
        return "pending";
    }

    private String auditJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException exception) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo dữ liệu audit", exception);
        }
    }
}
