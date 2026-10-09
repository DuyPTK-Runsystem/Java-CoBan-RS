package com.JavaTraining.BaiTap_RS.library.circulation.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "library_circulation_policy")
@Getter
@NoArgsConstructor
public class LibraryCirculationPolicy {

    private static final String IMMUTABLE_FIELD_WARNING = "PMD.ImmutableField";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long id;

    @Transient
    private LibraryCirculationPolicySnapshot snapshot;

    // Hibernate manages this persistent collection; aggregate methods mutate its contents, not its reference.
    @SuppressWarnings(IMMUTABLE_FIELD_WARNING)
    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("tierOrder ASC")
    private List<LibraryPolicyFineTier> fineTiers = new ArrayList<>();

    public LibraryCirculationPolicy(LibraryCirculationPolicySnapshot snapshot) {
        this.snapshot = snapshot;
    }

    @Embedded
    @Access(AccessType.PROPERTY)
    protected LibraryCirculationPolicySnapshot getSnapshot() {
        return snapshot;
    }

    protected void setSnapshot(LibraryCirculationPolicySnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public String getPolicyVersion() {
        return snapshot.getPolicyVersion();
    }

    public LocalDateTime getEffectiveAt() {
        return snapshot.getEffectiveAt();
    }

    public int getMaxActiveLoans() {
        return snapshot.getMaxActiveLoans();
    }

    public int getLoanDurationDays() {
        return snapshot.getLoanDurationDays();
    }

    public int getMaxRenewals() {
        return snapshot.getMaxRenewals();
    }

    public int getRenewalDurationDays() {
        return snapshot.getRenewalDurationDays();
    }

    public int getReservationPickupDays() {
        return snapshot.getReservationPickupDays();
    }

    public BigDecimal getFineCapPerLoan() {
        return snapshot.getFineCapPerLoan();
    }

    public BigDecimal getFineSuspensionThreshold() {
        return snapshot.getFineSuspensionThreshold();
    }

    public Long getUpdatedBy() {
        return snapshot.getUpdatedBy();
    }

    public LocalDateTime getUpdatedAt() {
        return snapshot.getUpdatedAt();
    }

    public void addTier(LibraryPolicyFineTier tier) {
        fineTiers.add(tier);
        tier.setPolicy(this);
    }

    public void replaceTiers(List<LibraryPolicyFineTier> tiers) {
        fineTiers.clear();
        tiers.forEach(this::addTier);
    }
}
