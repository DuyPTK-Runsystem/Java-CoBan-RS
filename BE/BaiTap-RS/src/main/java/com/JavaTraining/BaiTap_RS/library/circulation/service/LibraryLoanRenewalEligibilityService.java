package com.JavaTraining.BaiTap_RS.library.circulation.service;

import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoan;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LoanStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.security.LibraryAccessPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LibraryLoanRenewalEligibilityService {

    private static final String FORBIDDEN = "LIBRARY_RESOURCE_FORBIDDEN";
    private static final String CLOSED_LOAN = "ACTIVE_LOAN_NOT_FOUND";

    private final LibraryBorrowerEligibilityService borrowerEligibilityService;
    private final LibraryAccessPolicy accessPolicy;

    public LibraryLoanRenewalEligibilityService(LibraryBorrowerEligibilityService borrowerEligibilityService,
            LibraryAccessPolicy accessPolicy) {
        this.borrowerEligibilityService = borrowerEligibilityService;
        this.accessPolicy = accessPolicy;
    }

    public void validate(LibraryPatron patron, LibraryLoan loan) {
        if (!accessPolicy.isManagerOrOwner(patron.getUserId())) {
            throw error(HttpStatus.FORBIDDEN, FORBIDDEN, "Không có quyền gia hạn khoản mượn");
        }
        borrowerEligibilityService.validateRenewal(patron, accessPolicy.isManager());
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw error(HttpStatus.CONFLICT, CLOSED_LOAN, "Khoản mượn đã được đóng");
        }
    }

    private LibraryCirculationException error(HttpStatus status, String code, String message) {
        return new LibraryCirculationException(status, code, message);
    }
}
