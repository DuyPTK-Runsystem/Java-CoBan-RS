package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.FineStatus;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.FineType;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicySnapshot;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicyTerms;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryFine;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryLoan;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryFineRepository;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryLoanRepository;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronStatus;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronSuspension;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronSuspensionRepository;
import com.JavaTraining.BaiTap_RS.library.security.LibraryAccessPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LibraryFineSettlementBoundaryTest {

    private static final Long FINE_ID = 42L;
    private static final Long LOAN_ID = 21L;
    private static final Long PATRON_ID = 7L;

    @Mock private LibraryFineRepository fineRepository;
    @Mock private LibraryLoanRepository loanRepository;
    @Mock private LibraryPolicyService policyService;
    @Mock private LibraryFineCalculator calculator;
    @Mock private LibraryCirculationMapper mapper;
    @Mock private LibraryFineQueryService queryService;
    @Mock private LibraryPatronRepository patronRepository;
    @Mock private LibraryPatronSuspensionRepository suspensionRepository;
    @Mock private LibraryOperationAuditService auditService;
    @Mock private LibraryAccessPolicy accessPolicy;

    @InjectMocks
    private LibraryFineSettlementService settlementService;

    private LibraryFineService service;

    private LibraryFine provisionalFine;
    private LibraryLoan loan;
    private LibraryPatron patron;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        provisionalFine = new LibraryFine(LOAN_ID, FineType.OVERDUE, new BigDecimal("25000.00"), true,
                LocalDate.now(), "LIB-POL-1", now);
        ReflectionTestUtils.setField(provisionalFine, "id", FINE_ID);
        loan = new LibraryLoan(PATRON_ID, 31L, 51L, now.minusDays(20), now.minusDays(2),
                "LIB-POL-1", 5, 14, 2, 7);
        ReflectionTestUtils.setField(loan, "id", LOAN_ID);
        when(fineRepository.findById(FINE_ID)).thenReturn(Optional.of(provisionalFine));
        when(loanRepository.findById(LOAN_ID)).thenReturn(Optional.of(loan));
        patron = new LibraryPatron(90L, now);
        ReflectionTestUtils.setField(patron, "id", PATRON_ID);
        when(patronRepository.findByIdForUpdate(PATRON_ID)).thenReturn(Optional.of(patron));
        when(fineRepository.findByIdForUpdate(FINE_ID)).thenReturn(Optional.of(provisionalFine));
        service = new LibraryFineService(fineRepository, policyService, calculator, mapper, auditService,
                queryService, settlementService);
    }

    @Test
    void provisionalOverdueFineCannotBePaidBeforeItIsFinalized() {
        LibraryCirculationException exception = assertThrows(LibraryCirculationException.class,
                () -> service.pay(FINE_ID, "RECEIPT-42"));

        assertEquals("FINE_NOT_PAYABLE", exception.getCode());
        assertEquals(FineStatus.UNPAID, provisionalFine.getStatus());
        verify(policyService, never()).resolveCurrent(org.mockito.ArgumentMatchers.any());
        verify(auditService, never()).record(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void provisionalOverdueFineCannotBeWaivedBeforeItIsFinalized() {
        LibraryCirculationException exception = assertThrows(LibraryCirculationException.class,
                () -> service.waive(FINE_ID, "review pending"));

        assertEquals("FINE_NOT_WAIVABLE", exception.getCode());
        assertEquals(FineStatus.UNPAID, provisionalFine.getStatus());
        verify(policyService, never()).resolveCurrent(org.mockito.ArgumentMatchers.any());
        verify(auditService, never()).record(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void payingFineResolvesFineSuspensionAndActivatesPatronWhenNoOtherReasonRemains() {
        finalizeFine();
        LibraryPatronSuspension fineSuspension = new LibraryPatronSuspension(PATRON_ID, "Fine threshold", "FINE", now);
        when(policyService.resolveCurrent(org.mockito.ArgumentMatchers.any())).thenReturn(policy());
        when(fineRepository.sumByPatronAndStatus(PATRON_ID, FineStatus.UNPAID)).thenReturn(BigDecimal.ZERO);
        when(suspensionRepository.findAllByPatronIdAndResolvedAtIsNullAndSourceOrderBySuspendedAtAsc(PATRON_ID,
                "FINE")).thenReturn(List.of(fineSuspension));
        when(suspensionRepository.existsByPatronIdAndResolvedAtIsNull(PATRON_ID)).thenReturn(false);

        service.pay(FINE_ID, "RCPT-42");

        assertEquals(FineStatus.PAID, provisionalFine.getStatus());
        assertNotNull(fineSuspension.getResolvedAt());
        assertEquals(LibraryPatronStatus.ACTIVE, patron.getStatus());
    }

    @Test
    void waivingFineResolvesFineReasonButKeepsOtherSuspensionBlocking() {
        finalizeFine();
        ReflectionTestUtils.setField(patron, "status", LibraryPatronStatus.BORROWING_SUSPENDED);
        LibraryPatronSuspension fineSuspension = new LibraryPatronSuspension(PATRON_ID, "Fine threshold", "FINE", now);
        when(policyService.resolveCurrent(org.mockito.ArgumentMatchers.any())).thenReturn(policy());
        when(fineRepository.sumByPatronAndStatus(PATRON_ID, FineStatus.UNPAID)).thenReturn(BigDecimal.ZERO);
        when(suspensionRepository.findAllByPatronIdAndResolvedAtIsNullAndSourceOrderBySuspendedAtAsc(PATRON_ID,
                "FINE")).thenReturn(List.of(fineSuspension));
        when(suspensionRepository.existsByPatronIdAndResolvedAtIsNull(PATRON_ID)).thenReturn(true);

        service.waive(FINE_ID, "Approved correction");

        assertEquals(FineStatus.WAIVED, provisionalFine.getStatus());
        assertNotNull(fineSuspension.getResolvedAt());
        assertEquals(LibraryPatronStatus.BORROWING_SUSPENDED, patron.getStatus());
    }

    private void finalizeFine() {
        provisionalFine.updateAmount(provisionalFine.getAmount(), false, LocalDate.now(), "LIB-POL-1", now);
    }

    private LibraryCirculationPolicy policy() {
        LibraryCirculationPolicyTerms terms = new LibraryCirculationPolicyTerms(5, 14, 2, 7, 3,
                new BigDecimal("500000.00"), new BigDecimal("500000.00"));
        return new LibraryCirculationPolicy(new LibraryCirculationPolicySnapshot("LIB-POL-1", now, terms, 90L, now));
    }
}
