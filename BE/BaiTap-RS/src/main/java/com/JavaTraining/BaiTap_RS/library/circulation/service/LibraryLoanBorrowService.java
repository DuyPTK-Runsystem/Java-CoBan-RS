package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests.ReqBorrowCopiesDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryBorrowResultDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoan;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryReservation;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryLoanRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LibraryLoanBorrowService {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final LibraryLoanRepository loanRepository;
    private final BookCopyRepository copyRepository;
    private final LibraryCirculationMapper mapper;
    private final LibraryLoanBorrowRecordingService recordingService;
    private final LibraryLoanBorrowValidationService validationService;
    private final LibraryLoanBorrowPreparationService preparationService;

    @Transactional
    public LibraryBorrowResultDTO borrow(ReqBorrowCopiesDTO request) {
        LibraryLoanBorrowPreparationService.BorrowContext context = preparationService.prepare(request);
        LocalDateTime now = LocalDateTime.now(LIBRARY_ZONE);
        List<LibraryLoan> created = new ArrayList<>();
        for (BookCopy copy : context.copies()) {
            LibraryReservation ready = validationService.validateCopyForBorrow(copy, context.patron().getId());
            created.add(recordingService.createLoan(copy, context.patron(), context.cardId(), ready,
                    context.policy(), now));
        }
        try {
            loanRepository.flush();
            copyRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new LibraryCirculationException(HttpStatus.CONFLICT, "COPY_ALREADY_ON_LOAN",
                    "Bản sao vừa được mượn trong một giao dịch khác", exception);
        }
        return new LibraryBorrowResultDTO(created.stream().map(mapper::loan).toList());
    }

}
