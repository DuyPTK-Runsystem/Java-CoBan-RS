package com.JavaTraining.BaiTap_RS.library.circulation.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Access(AccessType.FIELD)
@Getter
@NoArgsConstructor
public class LibraryFineSettlement {

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_reference", length = 200)
    private String paymentReference;

    @Column(name = "paid_by")
    private Long paidBy;

    @Column(name = "waived_at")
    private LocalDateTime waivedAt;

    @Column(name = "waive_reason", length = 500)
    private String waiveReason;

    @Column(name = "waived_by")
    private Long waivedBy;

    public void pay(String reference, Long actorId, LocalDateTime now) {
        paymentReference = reference;
        paidBy = actorId;
        paidAt = now;
    }

    public void waive(String reason, Long actorId, LocalDateTime now) {
        waiveReason = reason;
        waivedBy = actorId;
        waivedAt = now;
    }
}
