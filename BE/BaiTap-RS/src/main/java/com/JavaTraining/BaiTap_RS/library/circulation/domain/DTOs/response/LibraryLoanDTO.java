package com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response;

import java.time.OffsetDateTime;

import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LoanStatus;

public record LibraryLoanDTO(Long loanId, Long patronId, Long copyId, String copyBarcode,
        Long bookId, String bookTitle, String cardNo, LoanStatus status, OffsetDateTime borrowedAt,
        OffsetDateTime dueAt, OffsetDateTime returnedAt, OffsetDateTime lostAt, int renewCount,
        String policyVersion) {
}
