package com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.FineStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.FineType;
import com.fasterxml.jackson.annotation.JsonFormat;

public record LibraryFineDTO(Long fineId, Long loanId, FineType type, FineStatus status,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount, String currency,
        LocalDate calculatedThrough, String policyVersion,
        OffsetDateTime paidAt, String paymentReference, OffsetDateTime waivedAt, String waiveReason,
        boolean provisional) {
}
