package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryLoanDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoan;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoanRenewal;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryLoanRenewalRepository;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import org.springframework.stereotype.Service;

@Service
public class LibraryLoanRenewalRecorderService {

    private final LibraryLoanRenewalRepository renewalRepository;
    private final LibraryCirculationMapper mapper;
    private final LibraryOperationAuditService auditService;

    public LibraryLoanRenewalRecorderService(LibraryLoanRenewalRepository renewalRepository,
            LibraryCirculationMapper mapper, LibraryOperationAuditService auditService) {
        this.renewalRepository = renewalRepository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    public LibraryLoanDTO renewAndRecord(LibraryLoan loan, LibraryCirculationPolicy policy, LocalDateTime now) {
        LocalDateTime previousDueAt = loan.getDueAt();
        LocalDateTime newDueAt = previousDueAt.toLocalDate().plusDays(policy.getRenewalDurationDays())
                .atTime(LocalTime.MAX);
        loan.renew(newDueAt, now);
        renewalRepository.save(new LibraryLoanRenewal(loan.getId(), loan.getRenewCount(),
                policy.getPolicyVersion(), policy.getRenewalDurationDays(), previousDueAt, newDueAt,
                AuditContext.currentUserId(), now));
        auditService.record("LOAN_RENEWED", "library_loan", loan.getId(),
                Map.of("dueAt", previousDueAt.toString()),
                Map.of("dueAt", newDueAt.toString(), "policyVersion", policy.getPolicyVersion()));
        return mapper.loan(loan);
    }
}
