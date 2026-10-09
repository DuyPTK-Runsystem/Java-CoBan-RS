package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryFineDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.FineStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryFine;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoan;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryFineRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryLoanRepository;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronStatus;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronSuspension;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronSuspensionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LibraryFineSettlementService {
    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String FINE_NOT_FOUND_CODE = "FINE_NOT_FOUND";
    private static final String FINE_NOT_FOUND_MESSAGE = "Không tìm thấy khoản phạt";
    private final LibraryFineRepository fineRepository;
    private final LibraryLoanRepository loanRepository;
    private final LibraryPolicyService policyService;
    private final LibraryCirculationMapper mapper;
    private final LibraryPatronRepository patronRepository;
    private final LibraryPatronSuspensionRepository suspensionRepository;
    private final LibraryOperationAuditService auditService;
    @Transactional
    public LibraryFineDTO pay(Long fineId, String reference) {
        LibraryFine initial = fineRepository.findById(fineId).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, FINE_NOT_FOUND_CODE, FINE_NOT_FOUND_MESSAGE));
        LibraryLoan initialLoan = findLoan(initial.getLoanId());
        patronRepository.findByIdForUpdate(initialLoan.getPatronId()).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, "PATRON_NOT_FOUND", "Không tìm thấy hồ sơ bạn đọc"));
        LibraryFine fine = fineRepository.findByIdForUpdate(fineId).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, FINE_NOT_FOUND_CODE, FINE_NOT_FOUND_MESSAGE));
        if (fine.isProvisional() || fine.getStatus() != FineStatus.UNPAID) {
            throw error(HttpStatus.CONFLICT, "FINE_NOT_PAYABLE", "Khoản phạt chưa chốt hoặc đã được xử lý");
        }
        fine.pay(reference.trim(), AuditContext.currentUserId(), LocalDateTime.now(LIBRARY_ZONE));
        LibraryLoan loan = findLoan(fine.getLoanId());
        recomputeSuspension(loan.getPatronId(), policyService.resolveCurrent(LocalDateTime.now(LIBRARY_ZONE)),
                AuditContext.currentUserId());
        auditService.record("FINE_PAID", "library_fine", fine.getId(), null,
                Map.of("reference", reference.trim(), "amount", fine.getAmount().toPlainString()));
        return mapper.fine(fine);
    }

    @Transactional
    public LibraryFineDTO waive(Long fineId, String reason) {
        LibraryFine initial = fineRepository.findById(fineId).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, FINE_NOT_FOUND_CODE, FINE_NOT_FOUND_MESSAGE));
        LibraryLoan initialLoan = findLoan(initial.getLoanId());
        patronRepository.findByIdForUpdate(initialLoan.getPatronId()).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, "PATRON_NOT_FOUND", "Không tìm thấy hồ sơ bạn đọc"));
        LibraryFine fine = fineRepository.findByIdForUpdate(fineId).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, FINE_NOT_FOUND_CODE, FINE_NOT_FOUND_MESSAGE));
        if (fine.isProvisional() || fine.getStatus() != FineStatus.UNPAID) {
            throw error(HttpStatus.CONFLICT, "FINE_NOT_WAIVABLE", "Khoản phạt chưa chốt hoặc đã được xử lý");
        }
        fine.waive(reason.trim(), AuditContext.currentUserId(), LocalDateTime.now(LIBRARY_ZONE));
        LibraryLoan loan = findLoan(fine.getLoanId());
        recomputeSuspension(loan.getPatronId(), policyService.resolveCurrent(LocalDateTime.now(LIBRARY_ZONE)),
                AuditContext.currentUserId());
        auditService.record("FINE_WAIVED", "library_fine", fine.getId(), null,
                Map.of("reason", reason.trim(), "amount", fine.getAmount().toPlainString()));
        return mapper.fine(fine);
    }

    public void recomputeSuspension(Long patronId, LibraryCirculationPolicy policy, Long actorId) {
        BigDecimal unpaid = fineRepository.sumByPatronAndStatus(patronId, FineStatus.UNPAID);
        LibraryPatron patron = patronRepository.findByIdForUpdate(patronId).orElse(null);
        if (patron == null || patron.getStatus() == LibraryPatronStatus.CLOSED) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(LIBRARY_ZONE);
        if (unpaid.compareTo(policy.getFineSuspensionThreshold()) > 0) {
            boolean already = suspensionRepository.existsByPatronIdAndResolvedAtIsNullAndSource(patronId, "FINE");
            if (!already) {
                suspensionRepository.save(new LibraryPatronSuspension(patronId,
                        "Unpaid library fines exceed the configured threshold", "FINE", now));
                auditService.record(actorId, "PATRON_SUSPENDED_FOR_FINE", "library_patron", patronId, null,
                        Map.of("unpaidTotal", unpaid.toPlainString(),
                                "threshold", policy.getFineSuspensionThreshold().toPlainString()));
            }
            patron.setStatus(LibraryPatronStatus.BORROWING_SUSPENDED, now);
            return;
        }
        List<LibraryPatronSuspension> fineSuspensions = suspensionRepository
                .findAllByPatronIdAndResolvedAtIsNullAndSourceOrderBySuspendedAtAsc(patronId, "FINE");
        fineSuspensions.forEach(item -> item.resolve(actorId, now));
        if (!fineSuspensions.isEmpty() && !suspensionRepository.existsByPatronIdAndResolvedAtIsNull(patronId)) {
            patron.setStatus(LibraryPatronStatus.ACTIVE, now);
            auditService.record(actorId, "PATRON_FINE_SUSPENSION_RESOLVED", "library_patron", patronId, null,
                    Map.of("unpaidTotal", unpaid.toPlainString()));
        }
    }

    private LibraryLoan findLoan(Long loanId) {
        return loanRepository.findById(loanId).orElseThrow(() -> error(
                HttpStatus.NOT_FOUND, "ACTIVE_LOAN_NOT_FOUND", "Không tìm thấy khoản mượn"));
    }

    private LibraryCirculationException error(HttpStatus status, String code, String message) {
        return new LibraryCirculationException(status, code, message);
    }
}
