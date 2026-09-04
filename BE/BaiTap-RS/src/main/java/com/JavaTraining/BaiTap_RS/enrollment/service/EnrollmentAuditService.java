package com.JavaTraining.BaiTap_RS.enrollment.service;

import java.util.Map;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.audit.domain.entity.AuditLog;
import com.JavaTraining.BaiTap_RS.common.audit.repository.AuditLogRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.ClassTransferHistory;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class EnrollmentAuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public EnrollmentAuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public void writeTransferAudit(
            Long actorUserId,
            StudentYearEnrollment enrollment,
            SchoolClass sourceClass,
            SchoolClass targetClass,
            ClassTransferHistory history) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        EnrollmentAuditService.class,
                        "EnrollmentAuditService.writeTransferAudit");
        auditLogRepository.save(new AuditLog(
                actorUserId,
                "STUDENT_ENROLLMENT_TRANSFER",
                "student_year_enrollment",
                enrollment.getId().toString(),
                beforeData(sourceClass, enrollment),
                afterData(targetClass, enrollment, history),
                AuditContext.requestId(),
                AuditContext.ipAddress()));
    }

    private String beforeData(SchoolClass sourceClass, StudentYearEnrollment enrollment) {
        return auditJson(Map.of(
                "currentClassId", sourceClass.getId(),
                "status", enrollment.getStatus().name()));
    }

    private String afterData(
            SchoolClass targetClass,
            StudentYearEnrollment enrollment,
            ClassTransferHistory history) {
        return auditJson(Map.of(
                "currentClassId", targetClass.getId(),
                "status", enrollment.getStatus().name(),
                "transferId", history.getId()));
    }

    private String auditJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException exception) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo dữ liệu audit", exception);
        }
    }
}
