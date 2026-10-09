package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests.ReqBorrowCopiesDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LibraryLoanBorrowPreparationService {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String COPY_NOT_FOUND = "COPY_NOT_FOUND";
    private static final String PATRON_NOT_FOUND = "PATRON_NOT_FOUND";

    private final BookCopyRepository copyRepository;
    private final BookRepository bookRepository;
    private final LibraryPatronRepository patronRepository;
    private final LibraryBorrowerEligibilityService borrowerEligibilityService;
    private final LibraryPolicyService policyService;
    private final LibraryLoanBorrowValidationService validationService;

    public LibraryLoanBorrowPreparationService(BookCopyRepository copyRepository, BookRepository bookRepository,
            LibraryPatronRepository patronRepository, LibraryBorrowerEligibilityService borrowerEligibilityService,
            LibraryPolicyService policyService, LibraryLoanBorrowValidationService validationService) {
        this.copyRepository = copyRepository;
        this.bookRepository = bookRepository;
        this.patronRepository = patronRepository;
        this.borrowerEligibilityService = borrowerEligibilityService;
        this.policyService = policyService;
        this.validationService = validationService;
    }

    public BorrowContext prepare(ReqBorrowCopiesDTO request) {
        List<String> barcodes = normalize(request.copyBarcodes());
        barcodes.stream().map(copyRepository::findBookIdByBarcode).flatMap(java.util.Optional::stream).distinct().sorted()
                .forEach(this::lockBook);
        LibraryPatron patron = patronRepository.findByIdForUpdate(request.patronId()).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, PATRON_NOT_FOUND, "Không tìm thấy hồ sơ bạn đọc"));
        Long cardId = borrowerEligibilityService.validateBorrower(patron, request.cardNo()).getId();
        List<BookCopy> copies = copyRepository.findAllByBarcodesForUpdate(barcodes);
        if (copies.size() != barcodes.size()) {
            throw error(HttpStatus.NOT_FOUND, COPY_NOT_FOUND, "Không tìm thấy một hoặc nhiều bản sao");
        }
        LibraryCirculationPolicy policy = policyService.resolveCurrent(LocalDateTime.now(LIBRARY_ZONE));
        validationService.validateLoanCapacity(patron, copies.size(), policy);
        return new BorrowContext(patron, cardId, copies, policy);
    }

    private List<String> normalize(List<String> values) {
        List<String> barcodes = values.stream().map(String::trim).toList();
        Set<String> unique = new HashSet<>(barcodes);
        if (unique.size() != barcodes.size() || unique.contains("")) {
            throw error(HttpStatus.BAD_REQUEST, "DUPLICATE_OR_EMPTY_BARCODE", "Barcode trùng hoặc để trống");
        }
        return barcodes;
    }

    private void lockBook(Long bookId) {
        bookRepository.findByIdForUpdate(bookId).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "Không tìm thấy đầu sách"));
    }

    private LibraryCirculationException error(HttpStatus status, String code, String message) {
        return new LibraryCirculationException(status, code, message);
    }

    public record BorrowContext(LibraryPatron patron, Long cardId, List<BookCopy> copies,
            LibraryCirculationPolicy policy) { }
}
