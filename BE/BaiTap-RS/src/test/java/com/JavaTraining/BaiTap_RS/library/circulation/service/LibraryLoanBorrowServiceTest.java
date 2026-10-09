package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.Book;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests.ReqBorrowCopiesDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryBorrowResultDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryLoanDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicySnapshot;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicyTerms;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoan;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LoanStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryLoanRepository;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LibraryLoanBorrowServiceTest {

    private static final Long PATRON_ID = 4L;
    private static final Long CARD_ID = 51L;

    @Mock private LibraryLoanRepository loanRepository;
    @Mock private BookCopyRepository copyRepository;
    @Mock private LibraryCirculationMapper mapper;
    @Mock private LibraryLoanBorrowRecordingService recordingService;
    @Mock private LibraryLoanBorrowValidationService validationService;
    @Mock private LibraryLoanBorrowPreparationService preparationService;

    @InjectMocks
    private LibraryLoanBorrowService service;

    @Test
    void recordsEveryPreparedCopyAndReturnsTheMappedLoanItems() {
        LibraryPatron patron = new LibraryPatron(15L, LocalDateTime.now());
        ReflectionTestUtils.setField(patron, "id", PATRON_ID);
        Book book = new Book("9780000000001", "Title", "Author", "Publisher", 2020, "Fiction",
                new BigDecimal("100000.00"), null);
        BookCopy firstCopy = copy(book, 31L, "COPY-31");
        BookCopy secondCopy = copy(book, 32L, "COPY-32");
        LibraryCirculationPolicy policy = policy();
        LibraryLoanBorrowPreparationService.BorrowContext context =
                new LibraryLoanBorrowPreparationService.BorrowContext(
                        patron, CARD_ID, List.of(firstCopy, secondCopy), policy);
        LibraryLoan firstLoan = loan(101L, firstCopy.getId());
        LibraryLoan secondLoan = loan(102L, secondCopy.getId());
        LibraryLoanDTO firstDto = loanDto(firstLoan.getId(), firstCopy.getId());
        LibraryLoanDTO secondDto = loanDto(secondLoan.getId(), secondCopy.getId());
        when(preparationService.prepare(any(ReqBorrowCopiesDTO.class))).thenReturn(context);
        when(recordingService.createLoan(eq(firstCopy), eq(patron), eq(CARD_ID), isNull(), eq(policy), any()))
                .thenReturn(firstLoan);
        when(recordingService.createLoan(eq(secondCopy), eq(patron), eq(CARD_ID), isNull(), eq(policy), any()))
                .thenReturn(secondLoan);
        when(mapper.loan(firstLoan)).thenReturn(firstDto);
        when(mapper.loan(secondLoan)).thenReturn(secondDto);

        LibraryBorrowResultDTO result = service.borrow(new ReqBorrowCopiesDTO(PATRON_ID, "CARD-4",
                List.of("COPY-31", "COPY-32")));

        assertEquals(List.of(firstDto, secondDto), result.items());
        verify(validationService).validateCopyForBorrow(firstCopy, PATRON_ID);
        verify(validationService).validateCopyForBorrow(secondCopy, PATRON_ID);
        verify(recordingService).createLoan(eq(firstCopy), eq(patron), eq(CARD_ID), isNull(), eq(policy), any());
        verify(recordingService).createLoan(eq(secondCopy), eq(patron), eq(CARD_ID), isNull(), eq(policy), any());
        verify(loanRepository).flush();
        verify(copyRepository).flush();
    }

    private BookCopy copy(Book book, Long id, String barcode) {
        BookCopy copy = new BookCopy(book, barcode, "Shelf", false);
        ReflectionTestUtils.setField(copy, "id", id);
        return copy;
    }

    private LibraryLoan loan(Long id, Long copyId) {
        LibraryLoan loan = new LibraryLoan(PATRON_ID, copyId, CARD_ID,
                LocalDateTime.now(), LocalDateTime.now().plusDays(14), "LIB-POL-1", 5, 14, 2, 7);
        ReflectionTestUtils.setField(loan, "id", id);
        return loan;
    }

    private LibraryLoanDTO loanDto(Long loanId, Long copyId) {
        LocalDateTime now = LocalDateTime.now();
        return new LibraryLoanDTO(loanId, PATRON_ID, copyId, "COPY-" + copyId, 101L, "Title", "CARD-4",
                LoanStatus.ACTIVE, now, now.plusDays(14), null, null, 0, "LIB-POL-1");
    }

    private LibraryCirculationPolicy policy() {
        LocalDateTime now = LocalDateTime.now();
        LibraryCirculationPolicyTerms terms = new LibraryCirculationPolicyTerms(5, 14, 2, 7, 3,
                new BigDecimal("500000.00"), new BigDecimal("500000.00"));
        return new LibraryCirculationPolicy(new LibraryCirculationPolicySnapshot(
                "LIB-POL-1", now, terms, 1L, now));
    }
}
