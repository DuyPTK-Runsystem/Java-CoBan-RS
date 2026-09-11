package com.JavaTraining.BaiTap_RS.placement.service.support;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.audit.domain.entity.AuditLog;
import com.JavaTraining.BaiTap_RS.common.audit.repository.AuditLogRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests.ReqCreateEnrollmentDTO;
import com.JavaTraining.BaiTap_RS.enrollment.service.EnrollmentService;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.response.ResPlacementSessionDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementResult;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementResultStatus;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSession;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSessionStatus;
import com.JavaTraining.BaiTap_RS.placement.repository.PlacementSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public final class PlacementConfirmationSupport {
    private final PlacementSessionRepository sessions;
    private final EnrollmentService enrollmentService;
    private final AuditLogRepository audits;
    private final PlacementSessionAccess access;

    public PlacementConfirmationSupport(PlacementSessionRepository sessions, EnrollmentService enrollmentService,
            AuditLogRepository audits, PlacementSessionAccess access) {
        this.sessions = sessions;
        this.enrollmentService = enrollmentService;
        this.audits = audits;
        this.access = access;
    }

    public Optional<ResPlacementSessionDTO> replayConfirmed(Long id, String idempotencyKey) {
        return sessions.findByConfirmIdempotencyKey(idempotencyKey)
                .map(existing -> replayForSameSession(existing, id));
    }

    public void createAutomaticEnrollments(PlacementSession session, List<PlacementResult> resultValues) {
        resultValues.stream().filter(result -> result.getResultStatus() == PlacementResultStatus.AUTO_ASSIGNED)
                .forEach(result -> enrollmentService.createEnrollment(new ReqCreateEnrollmentDTO(
                        result.getStudentId(), null, session.getAcademicYearId(), result.getTargetClassId(), null)));
    }

    public void audit(String action, Long id, String after) {
        audits.save(new AuditLog(AuditContext.currentUserId(), action, "placement_session", id.toString(), null, after,
                AuditContext.requestId(), AuditContext.ipAddress()));
    }

    private ResPlacementSessionDTO replayForSameSession(PlacementSession existing, Long requestedId) {
        if (!Objects.equals(existing.getId(), requestedId)) {
            throw new AppException(HttpStatus.CONFLICT, "Khóa xác nhận đã được dùng cho phiên khác");
        }
        if (existing.getStatus() != PlacementSessionStatus.CONFIRMED) {
            throw new AppException(HttpStatus.CONFLICT, "Khóa xác nhận không hợp lệ cho phiên này");
        }
        return access.response(existing, access.allResults(requestedId));
    }
}
