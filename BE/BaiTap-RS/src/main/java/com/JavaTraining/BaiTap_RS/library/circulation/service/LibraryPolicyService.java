package com.JavaTraining.BaiTap_RS.library.circulation.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.requests.ReqLibraryPolicyDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.DTOs.response.LibraryPolicyDTO;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicy;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicySnapshot;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryCirculationPolicyTerms;
import com.JavaTraining.BaiTap_RS.library.circulation.domain.entity.LibraryPolicyFineTier;
import com.JavaTraining.BaiTap_RS.library.circulation.exception.LibraryCirculationException;
import com.JavaTraining.BaiTap_RS.library.circulation.repository.LibraryPolicyRepository;
import com.JavaTraining.BaiTap_RS.library.common.service.LibraryOperationAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibraryPolicyService {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final LibraryPolicyRepository policyRepository;
    private final LibraryPolicyMapper mapper;
    private final LibraryOperationAuditService auditService;

    @Transactional(readOnly = true)
    public LibraryPolicyDTO current() {
        return mapper.toDto(policyRepository.findFirstByOrderByIdDesc().orElseThrow(() ->
                error(HttpStatus.SERVICE_UNAVAILABLE, "LIBRARY_POLICY_NOT_CONFIGURED",
                        "Chưa cấu hình chính sách thư viện")));
    }

    @Transactional
    public LibraryPolicyDTO update(ReqLibraryPolicyDTO request) {
        LibraryCirculationPolicy latest = policyRepository.findLatestForUpdate().orElseThrow(() ->
                error(HttpStatus.SERVICE_UNAVAILABLE, "LIBRARY_POLICY_NOT_CONFIGURED",
                        "Chưa cấu hình chính sách thư viện"));
        if (!latest.getPolicyVersion().equals(request.expectedVersion())) {
            throw error(HttpStatus.CONFLICT, "VERSION_CONFLICT", "Chính sách đã được cập nhật; hãy tải lại");
        }
        validateTiers(request.fineTiers());
        LocalDateTime now = LocalDateTime.now(LIBRARY_ZONE);
        if (request.effectiveAt().isBefore(now)) {
            throw error(HttpStatus.BAD_REQUEST, "INVALID_POLICY_EFFECTIVE_AT",
                    "Thời điểm hiệu lực không được nằm trong quá khứ");
        }
        Long actor = AuditContext.currentUserId();
        String version = "LIB-POL-" + UUID.randomUUID();
        LibraryCirculationPolicyTerms terms = new LibraryCirculationPolicyTerms(request.maxActiveLoans(),
                request.loanDurationDays(), request.maxRenewals(), request.renewalDurationDays(),
                request.reservationPickupDays(), request.fineCapPerLoan(), request.fineSuspensionThreshold());
        LibraryCirculationPolicySnapshot snapshot = new LibraryCirculationPolicySnapshot(version,
                request.effectiveAt(), terms, actor, now);
        LibraryCirculationPolicy policy = new LibraryCirculationPolicy(snapshot);
        for (int index = 0; index < request.fineTiers().size(); index++) {
            policy.addTier(toEntityTier(index, request.fineTiers().get(index)));
        }
        LibraryCirculationPolicy saved = policyRepository.saveAndFlush(policy);
        auditService.record("CIRCULATION_POLICY_UPDATED", "library_circulation_policy", saved.getId(),
                Map.of("policyVersion", latest.getPolicyVersion()),
                Map.of("policyVersion", version, "effectiveAt", request.effectiveAt().toString()));
        return mapper.toDto(saved);
    }

    public LibraryCirculationPolicy resolveCurrent(LocalDateTime now) {
        return policyRepository.findEffective(now).stream().findFirst().orElseThrow(() ->
                error(HttpStatus.SERVICE_UNAVAILABLE, "LIBRARY_POLICY_NOT_CONFIGURED",
                        "Chưa có chính sách thư viện có hiệu lực"));
    }

    public LibraryCirculationPolicy resolveByVersion(String version) {
        return policyRepository.findByVersion(version).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "LIBRARY_POLICY_NOT_FOUND", "Không tìm thấy phiên bản chính sách"));
    }

    private void validateTiers(List<ReqLibraryPolicyDTO.FineTier> tiers) {
        validateTierOrder(tiers);
        validateFinalTier(tiers);
    }

    private void validateTierOrder(List<ReqLibraryPolicyDTO.FineTier> tiers) {
        Integer previousEnd = 0;
        for (int index = 0; index < tiers.size(); index++) {
            ReqLibraryPolicyDTO.FineTier tier = tiers.get(index);
            validateTier(index, tiers.size(), tier, previousEnd);
            previousEnd = tier.throughDay() == null ? Integer.MAX_VALUE : tier.throughDay();
        }
    }

    private void validateTier(int index, int tierCount, ReqLibraryPolicyDTO.FineTier tier, Integer previousEnd) {
        if (tier.throughDay() != null && tier.throughDay() <= previousEnd) {
            throw error(HttpStatus.BAD_REQUEST, "INVALID_FINE_TIERS", "Mốc ngày phạt phải tăng dần");
        }
        if (tier.throughDay() == null && index != tierCount - 1) {
            throw error(HttpStatus.BAD_REQUEST, "INVALID_FINE_TIERS", "Bậc không giới hạn phải là bậc cuối");
        }
    }

    private void validateFinalTier(List<ReqLibraryPolicyDTO.FineTier> tiers) {
        if (tiers.get(tiers.size() - 1).throughDay() != null) {
            throw error(HttpStatus.BAD_REQUEST, "INVALID_FINE_TIERS", "Bậc cuối phải không giới hạn");
        }
    }

    private LibraryPolicyFineTier toEntityTier(int index, ReqLibraryPolicyDTO.FineTier tier) {
        return new LibraryPolicyFineTier(index + 1, tier.throughDay(), tier.dailyRate());
    }

    private LibraryCirculationException error(HttpStatus status, String code, String message) {
        return new LibraryCirculationException(status, code, message);
    }
}
