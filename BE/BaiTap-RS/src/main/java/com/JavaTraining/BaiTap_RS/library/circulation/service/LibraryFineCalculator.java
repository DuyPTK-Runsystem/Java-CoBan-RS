package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.math.BigDecimal;
import java.util.List;

import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryPolicyFineTier;
import org.springframework.stereotype.Component;

@Component
public class LibraryFineCalculator {

    public BigDecimal calculate(long overdueDays, List<LibraryPolicyFineTier> tiers, BigDecimal cap) {
        if (overdueDays <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        long previousEnd = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (LibraryPolicyFineTier tier : tiers) {
            long tierEnd = tier.getThroughDay() == null ? overdueDays
                    : Math.min(overdueDays, tier.getThroughDay());
            long daysInTier = Math.max(0, tierEnd - previousEnd);
            total = total.add(tier.getDailyRate().multiply(BigDecimal.valueOf(daysInTier)));
            if (tier.getThroughDay() == null || overdueDays <= tier.getThroughDay()) {
                break;
            }
            previousEnd = tier.getThroughDay();
        }
        return total.min(cap).setScale(2);
    }
}
