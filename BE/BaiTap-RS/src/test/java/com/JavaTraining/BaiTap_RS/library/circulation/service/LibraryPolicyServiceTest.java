package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests.ReqLibraryPolicyDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicySnapshot;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicyTerms;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryPolicyFineTier;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryPolicyRepository;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LibraryPolicyServiceTest {

    @Mock
    private LibraryPolicyRepository policyRepository;

    @Mock
    private LibraryPolicyMapper mapper;

    @Mock
    private LibraryOperationAuditService auditService;

    @InjectMocks
    private LibraryPolicyService service;

    @Test
    void rejectsStaleExpectedVersionBeforePersistingOrAuditing() {
        when(policyRepository.findLatestForUpdate()).thenReturn(Optional.of(policy("LIB-POL-2")));

        LibraryCirculationException exception = assertThrows(LibraryCirculationException.class,
                () -> service.update(request("LIB-POL-1", validTiers(), future())));

        assertEquals("VERSION_CONFLICT", exception.getCode());
        verify(policyRepository, never()).saveAndFlush(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void rejectsNonIncreasingOrNonTerminalUnboundedFineTierBeforePersisting() {
        when(policyRepository.findLatestForUpdate()).thenReturn(Optional.of(policy("LIB-POL-1")));
        List<ReqLibraryPolicyDTO.FineTier> invalidTiers = List.of(
                new ReqLibraryPolicyDTO.FineTier(7, new BigDecimal("5000.00")),
                new ReqLibraryPolicyDTO.FineTier(7, new BigDecimal("10000.00")),
                new ReqLibraryPolicyDTO.FineTier(null, new BigDecimal("20000.00")));

        LibraryCirculationException exception = assertThrows(LibraryCirculationException.class,
                () -> service.update(request("LIB-POL-1", invalidTiers, future())));

        assertEquals("INVALID_FINE_TIERS", exception.getCode());
        verify(policyRepository, never()).saveAndFlush(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void rejectsEffectiveTimeInThePastWithoutChangingTheCurrentPolicy() {
        when(policyRepository.findLatestForUpdate()).thenReturn(Optional.of(policy("LIB-POL-1")));

        LibraryCirculationException exception = assertThrows(LibraryCirculationException.class,
                () -> service.update(request("LIB-POL-1", validTiers(), LocalDateTime.of(2020, 1, 1, 0, 0))));

        assertEquals("INVALID_POLICY_EFFECTIVE_AT", exception.getCode());
        verify(policyRepository, never()).saveAndFlush(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void resolvesOnlyPoliciesEffectiveAtTheRequestedLibraryTime() {
        LibraryCirculationPolicy expected = policy("LIB-POL-1");
        LocalDateTime requestedTime = LocalDateTime.of(2026, 10, 9, 12, 0);
        when(policyRepository.findEffective(requestedTime)).thenReturn(List.of(expected));

        assertEquals(expected, service.resolveCurrent(requestedTime));
        verify(policyRepository).findEffective(requestedTime);
    }

    private ReqLibraryPolicyDTO request(String version, List<ReqLibraryPolicyDTO.FineTier> tiers,
            LocalDateTime effectiveAt) {
        return new ReqLibraryPolicyDTO(version, effectiveAt, 5, 14, 2, 7, 3, tiers,
                new BigDecimal("500000.00"), new BigDecimal("500000.00"));
    }

    private List<ReqLibraryPolicyDTO.FineTier> validTiers() {
        return List.of(new ReqLibraryPolicyDTO.FineTier(7, new BigDecimal("5000.00")),
                new ReqLibraryPolicyDTO.FineTier(30, new BigDecimal("10000.00")),
                new ReqLibraryPolicyDTO.FineTier(null, new BigDecimal("20000.00")));
    }

    private LocalDateTime future() {
        return LocalDateTime.now().plusDays(2);
    }

    private LibraryCirculationPolicy policy(String version) {
        LocalDateTime effectiveAt = future();
        LocalDateTime updatedAt = LocalDateTime.now();
        LibraryCirculationPolicyTerms terms = new LibraryCirculationPolicyTerms(5, 14, 2, 7, 3,
                new BigDecimal("500000.00"), new BigDecimal("500000.00"));
        LibraryCirculationPolicy policy = new LibraryCirculationPolicy(new LibraryCirculationPolicySnapshot(
                version, effectiveAt, terms, 1L, updatedAt));
        policy.addTier(new LibraryPolicyFineTier(1, 7, new BigDecimal("5000.00")));
        policy.addTier(new LibraryPolicyFineTier(2, 30, new BigDecimal("10000.00")));
        policy.addTier(new LibraryPolicyFineTier(3, null, new BigDecimal("20000.00")));
        return policy;
    }
}
