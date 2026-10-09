package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopyStatus;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryLoanDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoan;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LoanStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryLoanRepository;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibraryLostService {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final BookCopyRepository copyRepository;
    private final BookRepository bookRepository;
    private final LibraryLoanRepository loanRepository;
    private final LibraryPolicyService policyService;
    private final com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository patronRepository;
    private final LibraryFineService fineService;
    private final LibraryCirculationMapper mapper;
    private final LibraryOperationAuditService auditService;

    @Transactional
    public LibraryLoanDTO markLost(String barcode, String reason) {
        Long bookId = copyRepository.findBookIdByBarcode(barcode).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "COPY_NOT_FOUND", "Không tìm thấy bản sao"));
        bookRepository.findByIdForUpdate(bookId).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "Không tìm thấy đầu sách"));
        BookCopy copy = copyRepository.findByBarcodeForUpdate(barcode).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "COPY_NOT_FOUND", "Không tìm thấy bản sao"));
        LibraryLoan initialLoan = loanRepository.findFirstByCopyIdAndStatus(copy.getId(), LoanStatus.ACTIVE)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "ACTIVE_LOAN_NOT_FOUND",
                        "Không tìm thấy khoản mượn đang hoạt động"));
        patronRepository.findByIdForUpdate(initialLoan.getPatronId()).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "PATRON_NOT_FOUND", "Không tìm thấy hồ sơ bạn đọc"));
        LibraryLoan loan = loanRepository.findByIdForUpdate(initialLoan.getId()).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "ACTIVE_LOAN_NOT_FOUND", "Không tìm thấy khoản mượn"));
        if (copy.getBook().getListPrice() == null) {
            throw error(HttpStatus.CONFLICT, "LOST_ITEM_PRICE_MISSING", "Đầu sách chưa có giá bìa để tính phí mất sách");
        }
        LocalDateTime now = LocalDateTime.now(LIBRARY_ZONE);
        LibraryCirculationPolicy policy = policyService.resolveCurrent(now);
        loan.markLost(now);
        copy.setStatus(BookCopyStatus.LOST);
        fineService.recalculateOverdue(loan, now.toLocalDate(), false, policy);
        fineService.createLostFine(loan, copy.getBook().getListPrice(), now.toLocalDate(), policy);
        auditService.record("COPY_MARKED_LOST", "library_loan", loan.getId(), null,
                Map.of("copyId", copy.getId(), "reason", reason.trim(),
                        "actorUserId", AuditContext.currentUserId(), "lostAt", now.toString()));
        return mapper.loan(loan);
    }

    private LibraryCirculationException error(HttpStatus status, String code, String message) {
        return new LibraryCirculationException(status, code, message);
    }
}
