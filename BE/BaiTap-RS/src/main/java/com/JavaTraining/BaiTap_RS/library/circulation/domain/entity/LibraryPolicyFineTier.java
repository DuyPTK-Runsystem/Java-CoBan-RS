package com.JavaTraining.BaiTap_RS.library.circulation.domain.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "library_policy_fine_tier")
@Getter
@NoArgsConstructor
public class LibraryPolicyFineTier {

    private static final String IMMUTABLE_FIELD_WARNING = "PMD.ImmutableField";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fine_tier_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private LibraryCirculationPolicy policy;

    // Tier terms are immutable within a policy version; JPA field access requires non-final fields.
    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "tier_order", nullable = false)
    private int tierOrder;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "through_day")
    private Integer throughDay;

    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @Column(name = "rate_per_day", nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyRate;

    public LibraryPolicyFineTier(int tierOrder, Integer throughDay, BigDecimal dailyRate) {
        this.tierOrder = tierOrder;
        this.throughDay = throughDay;
        this.dailyRate = dailyRate;
    }

    /* package */
    void setPolicy(LibraryCirculationPolicy policy) {
        this.policy = policy;
    }
}
