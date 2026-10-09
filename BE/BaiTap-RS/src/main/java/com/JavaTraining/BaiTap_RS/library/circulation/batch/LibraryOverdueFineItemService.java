package com.JavaTraining.BaiTap_RS.library.circulation.batch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoan;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LoanStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryLoanRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.service.LibraryFineService;
import com.JavaTraining.BaiTap_RS.library.circulation.service.LibraryInAppNotificationService;
import com.JavaTraining.BaiTap_RS.library.circulation.service.LibraryPolicyService;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibraryOverdueFineItemService {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final LibraryLoanRepository loanRepository;
    private final LibraryPolicyService policyService;
    private final LibraryFineService fineService;
    private final BookCopyRepository copyRepository;
    private final LibraryPatronRepository patronRepository;
    private final LibraryInAppNotificationService notificationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long loanId, LocalDate runDate, Long actorUserId) {
        LibraryLoan initial = loanRepository.findById(loanId).orElseThrow(() ->
                new LibraryCirculationException(HttpStatus.NOT_FOUND, "ACTIVE_LOAN_NOT_FOUND",
                        "Loan no longer exists"));
        patronRepository.findByIdForUpdate(initial.getPatronId()).orElseThrow(() ->
                new LibraryCirculationException(HttpStatus.NOT_FOUND, "PATRON_NOT_FOUND",
                        "Patron no longer exists"));
        LibraryLoan loan = loanRepository.findByIdForUpdate(loanId).orElseThrow(() ->
                new LibraryCirculationException(HttpStatus.NOT_FOUND, "ACTIVE_LOAN_NOT_FOUND",
                        "Loan no longer exists"));
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            return;
        }
        if (!loan.getDueAt().toLocalDate().isBefore(runDate)) {
            return;
        }
        LocalDateTime asOf = LocalDateTime.of(runDate, LocalTime.MAX);
        LibraryCirculationPolicy policy = policyService.resolveCurrent(asOf);
        com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryFine fine =
                fineService.recalculateOverdue(loan, runDate, true, policy, actorUserId);
        if (fine != null) {
            com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy copy =
                    copyRepository.findById(loan.getCopyId()).orElseThrow();
            Long recipient = patronRepository.findById(loan.getPatronId()).orElseThrow().getUserId();
            notificationService.publish("library-overdue-fine-" + fine.getId() + "-" + runDate,
                    "Thông báo phí trễ hạn", "Khoản mượn “" + copy.getBook().getTitle()
                            + "” đang quá hạn; mức phí tạm tính " + fine.getAmount().toPlainString() + " VND.",
                    recipient, actorUserId);
        }
    }
}
