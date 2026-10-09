package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryFineDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.FineStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.FineType;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryFine;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoan;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryFineRepository;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LibraryFineService {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final LibraryFineRepository fineRepository;
    private final LibraryPolicyService policyService;
    private final LibraryFineCalculator calculator;
    private final LibraryCirculationMapper mapper;
    private final LibraryFineSettlementService settlementService;
    private final LibraryOperationAuditService auditService;
    private final LibraryFineQueryService queryService;

    public LibraryFineService(LibraryFineRepository fineRepository,
            LibraryPolicyService policyService,
            LibraryFineCalculator calculator, LibraryCirculationMapper mapper,
            LibraryOperationAuditService auditService,
            LibraryFineQueryService queryService, LibraryFineSettlementService settlementService) {
        this.fineRepository = fineRepository;
        this.policyService = policyService;
        this.calculator = calculator;
        this.mapper = mapper;
        this.settlementService = settlementService;
        this.auditService = auditService;
        this.queryService = queryService;
    }

    @Transactional
    public LibraryFine recalculateOverdue(LibraryLoan loan, LocalDate through, boolean provisional,
            LibraryCirculationPolicy policy) {
        return recalculateOverdue(loan, through, provisional, policy, AuditContext.currentUserId());
    }

    @Transactional
    public LibraryFine recalculateOverdue(LibraryLoan loan, LocalDate through, boolean provisional,
            LibraryCirculationPolicy policy, Long actorId) {
        long lateDays = Math.max(0, ChronoUnit.DAYS.between(loan.getDueAt().toLocalDate(), through));
        BigDecimal amount = calculator.calculate(lateDays, policy.getFineTiers(), policy.getFineCapPerLoan());
        if (lateDays == 0 && amount.signum() == 0) {
            return fineRepository.findByLoanIdAndType(loan.getId(), FineType.OVERDUE).orElse(null);
        }
        LibraryFine fine = fineRepository.findByLoanIdAndType(loan.getId(), FineType.OVERDUE).orElse(null);
        if (!shouldRecalculate(fine, through)) {
            return fine;
        }
        boolean created = fine == null;
        if (created) {
            fine = fineRepository.save(new LibraryFine(loan.getId(), FineType.OVERDUE,
                    amount, provisional, through, policy.getPolicyVersion(), LocalDateTime.now(LIBRARY_ZONE)));
        } else {
            fine.updateAmount(amount, provisional, through, policy.getPolicyVersion(), LocalDateTime.now(LIBRARY_ZONE));
        }
        fineRepository.flush();
        if (actorId != null && (created || fine.getStatus() == FineStatus.UNPAID)) {
            auditService.record(actorId, "OVERDUE_FINE_RECALCULATED", "library_fine", fine.getId(), null,
                    Map.of("loanId", loan.getId(), "amount", amount.toPlainString(),
                            "calculatedThrough", through.toString(), "policyVersion", policy.getPolicyVersion()));
        }
        settlementService.recomputeSuspension(loan.getPatronId(), policy, actorId);
        return fine;
    }

    @Transactional
    public LibraryFine createLostFine(LibraryLoan loan, BigDecimal listPrice, LocalDate through,
            LibraryCirculationPolicy policy) {
        BigDecimal amount = (listPrice == null ? BigDecimal.ZERO : listPrice).add(new BigDecimal("50000.00"));
        LibraryFine fine = fineRepository.findByLoanIdAndType(loan.getId(), FineType.LOST_ITEM)
                .orElseGet(() -> fineRepository.save(new LibraryFine(loan.getId(), FineType.LOST_ITEM,
                        amount, false, through, policy.getPolicyVersion(), LocalDateTime.now(LIBRARY_ZONE))));
        fine.updateAmount(amount, false, through, policy.getPolicyVersion(), LocalDateTime.now(LIBRARY_ZONE));
        fineRepository.flush();
        settlementService.recomputeSuspension(loan.getPatronId(), policy, AuditContext.currentUserId());
        return fine;
    }

    @Transactional(readOnly = true)
    public LibraryFineDTO get(Long fineId, Long patronId) {
        return queryService.get(fineId, patronId);
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO<LibraryFineDTO> page(Long patronId, FineStatus status, int page, int pageSize) {
        return queryService.page(patronId, status, page, pageSize);
    }

    @Transactional(readOnly = true)
    public LibraryFineDTO getForCurrentUser(Long fineId) {
        return queryService.getForCurrentUser(fineId);
    }

    @Transactional
    public LibraryFineDTO pay(Long fineId, String reference) {
        return settlementService.pay(fineId, reference);
    }

    @Transactional
    public LibraryFineDTO waive(Long fineId, String reason) {
        return settlementService.waive(fineId, reason);
    }

    private boolean shouldRecalculate(LibraryFine fine, LocalDate through) {
        return fine == null || fine.getStatus() == FineStatus.UNPAID
                && (fine.getCalculatedThrough() == null || fine.getCalculatedThrough().isBefore(through));
    }

}
