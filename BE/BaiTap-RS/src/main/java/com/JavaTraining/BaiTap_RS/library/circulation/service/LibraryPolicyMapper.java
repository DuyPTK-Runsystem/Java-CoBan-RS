package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryPolicyDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import org.springframework.stereotype.Component;

@Component
public class LibraryPolicyMapper {

    public LibraryPolicyDTO toDto(LibraryCirculationPolicy policy) {
        List<LibraryPolicyDTO.FineTier> tiers = policy.getFineTiers().stream()
                .map(tier -> new LibraryPolicyDTO.FineTier(tier.getThroughDay(), tier.getDailyRate()))
                .toList();
        return new LibraryPolicyDTO(policy.getPolicyVersion(), policy.getEffectiveAt(), policy.getMaxActiveLoans(),
                policy.getLoanDurationDays(), policy.getMaxRenewals(), policy.getRenewalDurationDays(),
                policy.getReservationPickupDays(), tiers, policy.getFineCapPerLoan(),
                policy.getFineSuspensionThreshold(), policy.getUpdatedAt(), policy.getUpdatedBy());
    }
}
