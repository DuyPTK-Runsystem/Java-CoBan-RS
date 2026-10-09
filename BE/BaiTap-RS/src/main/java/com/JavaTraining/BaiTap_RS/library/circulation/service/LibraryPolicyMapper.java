package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryPolicyDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import org.springframework.stereotype.Component;

@Component
public class LibraryPolicyMapper {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public LibraryPolicyDTO toDto(LibraryCirculationPolicy policy) {
        List<LibraryPolicyDTO.FineTier> tiers = policy.getFineTiers().stream()
                .map(tier -> new LibraryPolicyDTO.FineTier(tier.getThroughDay(), tier.getDailyRate()))
                .toList();
        return new LibraryPolicyDTO(policy.getPolicyVersion(), toOffsetDateTime(policy.getEffectiveAt()),
                policy.getMaxActiveLoans(), policy.getLoanDurationDays(), policy.getMaxRenewals(),
                policy.getRenewalDurationDays(), policy.getReservationPickupDays(), tiers,
                policy.getFineCapPerLoan(), policy.getFineSuspensionThreshold(),
                toOffsetDateTime(policy.getUpdatedAt()), policy.getUpdatedBy());
    }

    private static OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(LIBRARY_ZONE).toOffsetDateTime();
    }
}
