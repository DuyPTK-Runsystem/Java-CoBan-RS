package com.JavaTraining.BaiTap_RS.library.patron.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.requests.ReqUpdateLibraryPatronStatusDTO;
import com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.response.ResLibraryPatronDTO;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronStatus;
import com.JavaTraining.BaiTap_RS.library.patron.exception.LibraryPatronException;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LibraryPatronStatusService {

    private static final String ENTITY_TYPE = "library_patron";

    private final LibraryPatronRepository patronRepository;
    private final LibraryPatronReadModelService readModelService;
    private final LibraryPatronSuspensionService suspensionService;
    private final LibraryOperationAuditService auditService;

    public LibraryPatronStatusService(LibraryPatronRepository patronRepository,
            LibraryPatronReadModelService readModelService, LibraryPatronSuspensionService suspensionService,
            LibraryOperationAuditService auditService) {
        this.patronRepository = patronRepository;
        this.readModelService = readModelService;
        this.suspensionService = suspensionService;
        this.auditService = auditService;
    }

    @Transactional
    public ResLibraryPatronDTO updateStatus(Long patronId, ReqUpdateLibraryPatronStatusDTO request) {
        LibraryPatron patron = patronRepository.findByIdForUpdate(patronId).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, "PATRON_NOT_FOUND", "Không tìm thấy hồ sơ bạn đọc"));
        validateTransition(patron, request);
        String reason = request.reason() == null ? null : request.reason().trim();
        if (isIdempotentStatus(patron, request.status())) {
            return readModelService.patron(patron);
        }
        Map<String, Object> before = statusSnapshot(patron);
        LocalDateTime now = LocalDateTime.now();
        updateSuspensions(patron, request.status(), reason, now);
        patron.setStatus(request.status(), now);
        Map<String, Object> after = new HashMap<>(statusSnapshot(patron));
        after.put("changeReason", reason);
        auditService.record("LIBRARY_PATRON_STATUS_CHANGED", ENTITY_TYPE, patron.getId(), before, after);
        return readModelService.patron(patron);
    }

    private void validateTransition(LibraryPatron patron, ReqUpdateLibraryPatronStatusDTO request) {
        if (patron.getStatus() == LibraryPatronStatus.CLOSED && request.status() != LibraryPatronStatus.CLOSED) {
            throw error(HttpStatus.CONFLICT, "PATRON_STATUS_CONFLICT", "Hồ sơ đã đóng không thể mở lại");
        }
        if (requiresReason(request.status()) && isBlank(request.reason())) {
            throw error(HttpStatus.BAD_REQUEST, "PATRON_STATUS_REASON_REQUIRED", "Cần nhập lý do thay đổi trạng thái");
        }
    }

    private void updateSuspensions(LibraryPatron patron, LibraryPatronStatus status, String reason,
            LocalDateTime now) {
        if (status == LibraryPatronStatus.BORROWING_SUSPENDED) {
            suspensionService.suspendManually(patron.getId(), reason, now);
        } else if (status == LibraryPatronStatus.ACTIVE) {
            suspensionService.resolveAll(patron.getId(), AuditContext.currentUserId(), now);
        }
    }

    private boolean isIdempotentStatus(LibraryPatron patron, LibraryPatronStatus requested) {
        return patron.getStatus() == requested && requested != LibraryPatronStatus.BORROWING_SUSPENDED;
    }

    private boolean requiresReason(LibraryPatronStatus status) {
        return status == LibraryPatronStatus.BORROWING_SUSPENDED || status == LibraryPatronStatus.CLOSED;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Map<String, Object> statusSnapshot(LibraryPatron patron) {
        return Map.of("patronId", patron.getId(), "userId", patron.getUserId(), "status", patron.getStatus().name(),
                "suspensionReasons", readModelService.suspensionReasons(patron.getId()));
    }

    private LibraryPatronException error(HttpStatus status, String code, String message) {
        return new LibraryPatronException(status, code, message);
    }
}
