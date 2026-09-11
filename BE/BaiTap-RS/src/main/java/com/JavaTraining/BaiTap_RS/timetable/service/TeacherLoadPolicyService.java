package com.JavaTraining.BaiTap_RS.timetable.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqCreatePolicyDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTeacherLoadPolicyDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadPolicy;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadPolicyStatus;
import com.JavaTraining.BaiTap_RS.timetable.repository.TeacherLoadPolicyRepository;
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
        return toDTO(policy);
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
                p.getUpdatedAt());
    }
}
