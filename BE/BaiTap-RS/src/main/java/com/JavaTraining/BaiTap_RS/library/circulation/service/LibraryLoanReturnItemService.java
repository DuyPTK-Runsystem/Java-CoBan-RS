package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryReturnItemDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryFine;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoan;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LoanStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryLoanRepository;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LibraryLoanReturnItemService {

    private final LibraryLoanRepository loanRepository;
    private final LibraryFineService fineService;
    private final LibraryCirculationMapper mapper;
    private final LibraryReservationAllocationService allocationService;
    private final LibraryOperationAuditService auditService;

    public LibraryLoanReturnItemService(LibraryLoanRepository loanRepository,
            LibraryFineService fineService, LibraryCirculationMapper mapper,
            LibraryReservationAllocationService allocationService, LibraryOperationAuditService auditService) {
        this.loanRepository = loanRepository;
        this.fineService = fineService;
        this.mapper = mapper;
        this.allocationService = allocationService;
        this.auditService = auditService;
    }

    public LibraryReturnItemDTO returnCopy(BookCopy copy, LocalDate today, LocalDateTime now,
            LibraryCirculationPolicy policy) {
        LibraryLoan loan = loanRepository.findFirstByCopyIdAndStatus(copy.getId(), LoanStatus.ACTIVE)
                .orElseThrow(() -> new LibraryCirculationException(HttpStatus.NOT_FOUND, "ACTIVE_LOAN_NOT_FOUND",
                        "Không tìm thấy khoản mượn đang hoạt động cho barcode này"));
        loan.returnAt(now);
        copy.setStatus(BookCopyStatus.AVAILABLE);
        LibraryFine fine = fineService.recalculateOverdue(loan, today, false, policy);
        allocationService.allocateNext(copy, now, AuditContext.currentUserId());
        auditService.record("LOAN_RETURNED", "library_loan", loan.getId(), null,
                Map.of("copyId", copy.getId(), "returnedAt", now.toString()));
        return new LibraryReturnItemDTO(mapper.loan(loan), mapper.fine(fine));
    }
}
