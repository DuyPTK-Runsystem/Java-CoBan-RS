package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.math.BigDecimal;
import java.util.List;

import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryPolicyFineTier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LibraryFineCalculatorTest {

    private final LibraryFineCalculator calculator = new LibraryFineCalculator();
    private final List<LibraryPolicyFineTier> tiers = List.of(
            new LibraryPolicyFineTier(1, 7, new BigDecimal("5000.00")),
            new LibraryPolicyFineTier(2, 30, new BigDecimal("10000.00")),
            new LibraryPolicyFineTier(3, null, new BigDecimal("20000.00")));

    @Test
    void calculatesConfiguredTierBoundariesWithoutOffByOneDays() {
        assertAmount("0.00", 0);
        assertAmount("5000.00", 1);
        assertAmount("35000.00", 7);
        assertAmount("45000.00", 8);
        assertAmount("265000.00", 30);
        assertAmount("285000.00", 31);
    }

    @Test
    void capsTheCumulativeTierAmountAtConfiguredPerLoanLimit() {
        assertEquals(new BigDecimal("500000.00"), calculator.calculate(100, tiers,
                new BigDecimal("500000.00")));
        assertEquals(new BigDecimal("100.00"), calculator.calculate(1, tiers,
                new BigDecimal("100.00")));
    }

    @Test
    void returnsZeroForNegativeOverdueDays() {
        assertAmount("0.00", -3);
    }

    private void assertAmount(String expected, long days) {
        assertEquals(new BigDecimal(expected), calculator.calculate(days, tiers, new BigDecimal("500000.00")));
    }
}
