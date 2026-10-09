package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoan;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryReservation;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryLoanRepository;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import org.springframework.stereotype.Service;

@Service
public class LibraryLoanBorrowRecordingService {

    private final LibraryLoanRepository loanRepository;
    private final LibraryOperationAuditService auditService;

    public LibraryLoanBorrowRecordingService(LibraryLoanRepository loanRepository,
            LibraryOperationAuditService auditService) {
        this.loanRepository = loanRepository;
        this.auditService = auditService;
    }

    public LibraryLoan createLoan(BookCopy copy, LibraryPatron patron, Long cardId, LibraryReservation ready,
            LibraryCirculationPolicy policy, LocalDateTime now) {
        LocalDateTime dueAt = LocalDateTime.of(now.toLocalDate().plusDays(policy.getLoanDurationDays()), LocalTime.MAX);
        LibraryLoan loan = loanRepository.save(new LibraryLoan(patron.getId(), copy.getId(), cardId,
                now, dueAt, policy.getPolicyVersion(), policy.getMaxActiveLoans(), policy.getLoanDurationDays(),
                policy.getMaxRenewals(), policy.getRenewalDurationDays()));
        copy.setStatus(BookCopyStatus.ON_LOAN);
        if (ready != null) {
            ready.fulfill(now);
            auditService.record("RESERVATION_FULFILLED", "library_reservation", ready.getId(), null,
                    Map.of("loanId", loan.getId()));
        }
        auditService.record("LOAN_CREATED", "library_loan", loan.getId(), null,
                Map.of("patronId", patron.getId(), "copyId", copy.getId(), "dueAt", dueAt.toString()));
        return loan;
    }
}
