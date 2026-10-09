package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.util.List;
import java.util.Optional;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoan;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LoanStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryLoanRepository;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LibraryLoanReturnPreparationService {

    private static final String COPY_NOT_FOUND = "COPY_NOT_FOUND";
    private static final String ACTIVE_LOAN_NOT_FOUND = "ACTIVE_LOAN_NOT_FOUND";

    private final LibraryLoanRepository loanRepository;
    private final BookCopyRepository copyRepository;
    private final BookRepository bookRepository;
    private final LibraryPatronRepository patronRepository;

    public LibraryLoanReturnPreparationService(LibraryLoanRepository loanRepository,
            BookCopyRepository copyRepository, BookRepository bookRepository,
            LibraryPatronRepository patronRepository) {
        this.loanRepository = loanRepository;
        this.copyRepository = copyRepository;
        this.bookRepository = bookRepository;
        this.patronRepository = patronRepository;
    }

    public List<BookCopy> lockCopiesForReturn(List<String> barcodes) {
        barcodes.stream().map(copyRepository::findBookIdByBarcode).flatMap(Optional::stream)
                .distinct().sorted().forEach(this::lockBook);
        List<BookCopy> unlockedCopies = copyRepository.findAllByBarcodeIn(barcodes);
        if (unlockedCopies.size() != barcodes.size()) {
            throw error(HttpStatus.NOT_FOUND, COPY_NOT_FOUND, "Không tìm thấy một hoặc nhiều bản sao");
        }
        List<Long> copyIds = unlockedCopies.stream().map(BookCopy::getId).sorted().toList();
        List<LibraryLoan> initialLoans = loanRepository.findAllByCopyIdInAndStatus(copyIds, LoanStatus.ACTIVE);
        if (initialLoans.size() != copyIds.size()) {
            throw error(HttpStatus.NOT_FOUND, ACTIVE_LOAN_NOT_FOUND,
                    "Không tìm thấy khoản mượn đang hoạt động");
        }
        List<Long> patronIds = initialLoans.stream()
                .map(LibraryLoan::getPatronId)
                .distinct().sorted().toList();
        patronRepository.findAllByIdsForUpdate(patronIds);
        return copyRepository.findAllByBarcodesForUpdate(barcodes);
    }

    private void lockBook(Long bookId) {
        bookRepository.findByIdForUpdate(bookId).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "Không tìm thấy đầu sách"));
    }

    private LibraryCirculationException error(HttpStatus status, String code, String message) {
        return new LibraryCirculationException(status, code, message);
    }
}
