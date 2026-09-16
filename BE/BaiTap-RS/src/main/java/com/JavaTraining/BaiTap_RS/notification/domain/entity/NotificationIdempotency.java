package com.JavaTraining.BaiTap_RS.notification.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class NotificationIdempotency {

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String key;

    @Column(name = "idempotency_fingerprint", length = 64)
    private String fingerprint;
}
