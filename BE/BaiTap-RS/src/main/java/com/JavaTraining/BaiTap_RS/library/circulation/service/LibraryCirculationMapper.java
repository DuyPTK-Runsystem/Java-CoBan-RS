package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCard;
import com.JavaTraining.BaiTap_RS.library.card.repository.LibraryCardRepository;
import com.JavaTraining.BaiTap_RS.library.catalog.domain.entity.BookCopy;
import com.JavaTraining.BaiTap_RS.library.catalog.repository.BookCopyRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryFineDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryLoanDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryReservationDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryFine;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoan;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.FineType;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryReservation;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryFineRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class LibraryCirculationMapper {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BookCopyRepository copyRepository;
    private final LibraryCardRepository cardRepository;
    private final LibraryFineRepository fineRepository;

    public LibraryCirculationMapper(BookCopyRepository copyRepository, LibraryCardRepository cardRepository,
            LibraryFineRepository fineRepository) {
        this.copyRepository = copyRepository;
        this.cardRepository = cardRepository;
        this.fineRepository = fineRepository;
    }

    public LibraryLoanDTO loan(LibraryLoan loan) {
        BookCopy copy = copyRepository.findById(loan.getCopyId()).orElseThrow(() ->
                error("COPY_NOT_FOUND", "Không tìm thấy bản sao"));
        LibraryCard card = cardRepository.findById(loan.getCardId()).orElseThrow(() ->
                error("CARD_NOT_FOUND", "Không tìm thấy thẻ thư viện"));
        return new LibraryLoanDTO(loan.getId(), loan.getPatronId(), loan.getCopyId(), copy.getBarcode(),
                copy.getBook().getId(), copy.getBook().getTitle(), card.getCardNo(), loan.getStatus(),
                toOffsetDateTime(loan.getBorrowedAt()), toOffsetDateTime(loan.getDueAt()),
                toOffsetDateTime(loan.getReturnedAt()), toOffsetDateTime(loan.getLostAt()),
                loan.getRenewCount(), loan.getPolicyVersion());
    }

    public LibraryFineDTO fine(LibraryFine fine) {
        return fine == null ? null : new LibraryFineDTO(fine.getId(), fine.getLoanId(), fine.getType(),
                fine.getStatus(), fine.getAmount(), fine.getCurrency(), fine.getCalculatedThrough(),
                fine.getPolicyVersion(), toOffsetDateTime(fine.getPaidAt()), fine.getPaymentReference(),
                toOffsetDateTime(fine.getWaivedAt()), fine.getWaiveReason(), fine.isProvisional());
    }

    public LibraryReservationDTO reservation(LibraryReservation reservation) {
        BookCopy copy = reservation.getAllocatedCopyId() == null ? null
                : copyRepository.findById(reservation.getAllocatedCopyId()).orElse(null);
        String title = copy != null ? copy.getBook().getTitle() : copyRepository.findFirstByBookId(reservation.getBookId())
                .map(item -> item.getBook().getTitle()).orElse("");
        return new LibraryReservationDTO(reservation.getId(), reservation.getBookId(), title,
                reservation.getPatronId(), reservation.getStatus(),
                toOffsetDateTime(reservation.getReservedAt()), toOffsetDateTime(reservation.getReadyAt()),
                toOffsetDateTime(reservation.getPickupDueAt()), copy == null ? null : copy.getBarcode(),
                toOffsetDateTime(reservation.getFulfilledAt()), toOffsetDateTime(reservation.getCancelledAt()),
                reservation.getPolicyVersion());
    }

    private static OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(LIBRARY_ZONE).toOffsetDateTime();
    }

    public LibraryFine findFine(Long loanId) {
        return fineRepository.findByLoanIdAndType(loanId,
                FineType.OVERDUE).orElse(null);
    }

    private LibraryCirculationException error(String code, String message) {
        return new LibraryCirculationException(HttpStatus.NOT_FOUND, code, message);
    }
}
