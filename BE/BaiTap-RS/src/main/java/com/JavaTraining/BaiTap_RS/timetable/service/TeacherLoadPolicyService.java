package com.JavaTraining.BaiTap_RS.timetable.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqCreatePolicyDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqCreateTeacherLoadRuleDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTeacherLoadPolicyDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTeacherLoadRuleDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadPolicy;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadPolicyStatus;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadRule;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadRuleTriggerType;
import com.JavaTraining.BaiTap_RS.timetable.repository.TeacherLoadPolicyRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TeacherLoadRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeacherLoadPolicyService {

    private final TeacherLoadPolicyRepository policyRepository;
    private final TeacherLoadRuleRepository ruleRepository;

    @Transactional(readOnly = true)
    public ResultPaginationDTO<ResTeacherLoadPolicyDTO> pagePolicies(Pageable pageable) {
        Page<TeacherLoadPolicy> page = policyRepository.findAllByOrderByCreatedAtDesc(pageable);
        List<ResTeacherLoadPolicyDTO> dtos = page.getContent().stream()
                .map(this::toDTO)
                .toList();
        return new ResultPaginationDTO<>(
                new ResultPaginationDTO.Meta(page.getNumber(), page.getSize(),
                        page.getTotalPages(), page.getTotalElements()),
                dtos);
    }

    @Transactional(readOnly = true)
    public ResTeacherLoadPolicyDTO get(Long id) {
        return toDTO(findPolicy(id));
    }

    @Transactional(readOnly = true)
    public Optional<TeacherLoadPolicy> getActivePolicy() {
        return policyRepository.findFirstByStatusOrderByEffectiveFromDesc(TeacherLoadPolicyStatus.ACTIVE);
    }

    @Transactional
    public ResTeacherLoadPolicyDTO create(ReqCreatePolicyDTO req) {
        if (policyRepository.existsByVersion(req.version())) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Phiên bản định mức tiết dạy '" + req.version() + "' đã tồn tại");
        }
        TeacherLoadPolicy policy = new TeacherLoadPolicy(
                req.version(),
                req.source(),
                req.effectiveFrom(),
                req.effectiveTo(),
                req.basePeriods(),
                req.homeroomReduction(),
                req.nursingReduction());
        policy = policyRepository.save(policy);
        saveRules(policy, req.rules());
        return toDTO(policy);
    }

    @Transactional(readOnly = true)
    public List<ResTeacherLoadRuleDTO> listRules(Long policyId) {
        TeacherLoadPolicy policy = findPolicy(policyId);
        return rulesFor(policy).stream().map(this::toRuleDTO).toList();
    }

    @Transactional
    public ResTeacherLoadRuleDTO addRule(Long policyId, ReqCreateTeacherLoadRuleDTO req) {
        TeacherLoadPolicy policy = findPolicy(policyId);
        if (policy.getStatus() == TeacherLoadPolicyStatus.ACTIVE) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Không thể sửa policy đang kích hoạt. Hãy tạo phiên bản mới.");
        }
        String code = req.ruleCode().trim().toUpperCase(Locale.ROOT);
        if (ruleRepository.existsByPolicyIdAndRuleCode(policyId, code)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Mã rule đã tồn tại trong policy");
        }
        TeacherLoadRule rule = ruleRepository.save(new TeacherLoadRule(policyId, code,
                req.ruleName().trim(), req.triggerType(), req.reductionPeriods(), req.source().trim()));
        return toRuleDTO(rule);
    }

    @Transactional
    public ResTeacherLoadPolicyDTO activate(Long id, Long expectedVersion) {
        TeacherLoadPolicy policy = findPolicy(id);
        if (expectedVersion != null && !Objects.equals(policy.getVersionLock(), expectedVersion)) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Chính sách đã bị thay đổi bởi người khác. Vui lòng tải lại.");
        }

        // Archive previous active policies
        policyRepository.findFirstByStatusOrderByEffectiveFromDesc(TeacherLoadPolicyStatus.ACTIVE)
                .ifPresent(prev -> {
                    prev.setStatus(TeacherLoadPolicyStatus.ARCHIVED);
                    policyRepository.save(prev);
                });

        policy.setStatus(TeacherLoadPolicyStatus.ACTIVE);
        policy = policyRepository.save(policy);
        return toDTO(policy);
    }

    private TeacherLoadPolicy findPolicy(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy chính sách định mức"));
    }

    private ResTeacherLoadPolicyDTO toDTO(TeacherLoadPolicy p) {
        return new ResTeacherLoadPolicyDTO(
                p.getId(),
                p.getVersion(),
                p.getSource(),
                p.getEffectiveFrom(),
                p.getEffectiveTo(),
                p.getBasePeriods(),
                p.getHomeroomReduction(),
                p.getNursingReduction(),
                p.getStatus(),
                p.getVersionLock(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                rulesFor(p).stream().map(this::toRuleDTO).toList());
    }

    private void saveRules(TeacherLoadPolicy policy, List<ReqCreateTeacherLoadRuleDTO> requestedRules) {
        List<ReqCreateTeacherLoadRuleDTO> rules = requestedRules == null || requestedRules.isEmpty()
                ? List.of(
                        new ReqCreateTeacherLoadRuleDTO("HOMEROOM", "Giảm chủ nhiệm",
                                TeacherLoadRuleTriggerType.HOMEROOM,
                                policy.getHomeroomReduction(), policy.getSource()),
                        new ReqCreateTeacherLoadRuleDTO("NURSING_CHILD_UNDER_12M",
                                "Nuôi con nhỏ dưới 12 tháng",
                                TeacherLoadRuleTriggerType.ELIGIBILITY, policy.getNursingReduction(), policy.getSource()))
                : requestedRules;
        List<String> codes = new ArrayList<>();
        List<TeacherLoadRule> entities = rules.stream().map(req -> {
            String code = req.ruleCode().trim().toUpperCase(Locale.ROOT);
            if (!codes.add(code)) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Mã rule bị trùng trong policy");
            }
            return new TeacherLoadRule(policy.getId(), code, req.ruleName().trim(),
                    req.triggerType(), req.reductionPeriods(), req.source().trim());
        }).toList();
        ruleRepository.saveAll(entities);
    }

    private List<TeacherLoadRule> rulesFor(TeacherLoadPolicy policy) {
        List<TeacherLoadRule> rules = ruleRepository.findAllByPolicyIdOrderByIdAsc(policy.getId());
        if (!rules.isEmpty()) {
            return rules;
        }
        return List.of(
                new TeacherLoadRule(policy.getId(), "HOMEROOM", "Giảm chủ nhiệm", TeacherLoadRuleTriggerType.HOMEROOM,
                        policy.getHomeroomReduction(), policy.getSource()),
                new TeacherLoadRule(policy.getId(), "NURSING_CHILD_UNDER_12M",
                        "Nuôi con nhỏ dưới 12 tháng",
                        TeacherLoadRuleTriggerType.ELIGIBILITY, policy.getNursingReduction(), policy.getSource()));
    }

    private ResTeacherLoadRuleDTO toRuleDTO(TeacherLoadRule rule) {
        return new ResTeacherLoadRuleDTO(rule.getId(), rule.getPolicyId(), rule.getRuleCode(), rule.getRuleName(),
                rule.getTriggerType(), rule.getReductionPeriods(), rule.getSource(), rule.isActive());
    }
}
