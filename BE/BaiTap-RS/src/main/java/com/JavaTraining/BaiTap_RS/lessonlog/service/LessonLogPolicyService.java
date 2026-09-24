package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.ReqPolicyDTO;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogPolicyResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogPolicy;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogRevision;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogPolicyRepository;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogRevisionRepository;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;

/** Owns policy validation, versioning and deadline calculations for lesson logs. */
@Service
@RequiredArgsConstructor
public class LessonLogPolicyService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final LessonLogPolicyRepository policies;
    private final LessonLogRevisionRepository audits;
    private final ObjectMapper objectMapper;
    private final LessonLogPolicyValidator validator;

    @Transactional(readOnly = true)
    public LessonLogPolicy getEffectivePolicy(LocalDate lessonDate) {
        return policies.findTopByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(lessonDate)
                .orElseThrow(() -> error(HttpStatus.UNPROCESSABLE_ENTITY, "Chưa cấu hình policy sổ đầu bài"));
    }

    @Transactional(readOnly = true)
    public Optional<LessonLogPolicy> findById(Long policyId) {
        return policies.findById(policyId);
    }

    @Transactional(readOnly = true)
    public Optional<LessonLogPolicy> findEffectivePolicy(LocalDate lessonDate) {
        return policies.findTopByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(lessonDate);
    }

    @Transactional(readOnly = true)
    public LessonLogPolicyResponse getPolicy(LocalDate lessonDate) {
        LessonLogPolicy selectedPolicy = lessonDate == null
                ? policies.findTopByOrderByPolicyVersionDesc()
                        .orElseThrow(() -> error(HttpStatus.UNPROCESSABLE_ENTITY, "Chưa cấu hình policy sổ đầu bài"))
                : getEffectivePolicy(lessonDate);
        return toResponse(selectedPolicy);
    }

    @Transactional
    public LessonLogPolicyResponse putPolicy(ReqPolicyDTO request) {
        LessonLogPolicy previousPolicy = policies.findTopByOrderByPolicyVersionDesc().orElse(null);
        if (previousPolicy != null && !java.util.Objects.equals(previousPolicy.getVersion(), request.expectedVersion())) {
            throw error(HttpStatus.CONFLICT, "Policy đã thay đổi");
        }
        validator.validate(request, previousPolicy == null ? null : previousPolicy.getEffectiveFrom());

        LessonLogPolicy policy = new LessonLogPolicy();
        policy.setPolicyVersion(previousPolicy == null ? 1 : previousPolicy.getPolicyVersion() + 1);
        policy.setEffectiveFrom(request.effectiveFrom());
        policy.setTimezone(request.timezone());
        policy.setDeadlineMode(request.deadlineMode());
        policy.setEditWindowHours(request.editWindowHours());
        policy.setRequireHomeroomReview(!Boolean.FALSE.equals(request.requireHomeroomReview()));
        policy.setRubricJson(request.rubric());
        policy.setCreatedBy(actor());
        policy = policies.save(policy);
        audits.save(new LessonLogRevision(null, null, policy.getId(), "POLICY_CREATE", actor(), request.reason(), null,
                policyState(policy)));
        return toResponse(policy);
    }

    public LocalDateTime lessonEndsAt(LocalDate lessonDate, TimetablePeriod period) {
        return LocalDateTime.of(lessonDate, period.getEndTime());
    }

    public LocalDateTime editWindowExpiresAt(LessonLogPolicy policy, LocalDateTime lessonEndsAt) {
        return "END_OF_WEEK".equals(policy.getDeadlineMode())
                ? lessonEndsAt.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(1)
                        .atStartOfDay()
                : lessonEndsAt.plusHours(policy.getEditWindowHours());
    }

    public LessonLogPolicyResponse toResponse(LessonLogPolicy policy) {
        return new LessonLogPolicyResponse(policy.getId(), policy.getPolicyVersion(), policy.getVersion(),
                policy.getEffectiveFrom(), policy.getTimezone(), policy.getDeadlineMode(), policy.getEditWindowHours(),
                policy.isRequireHomeroomReview(), policy.getRubricJson());
    }

    private String policyState(LessonLogPolicy policy) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("policyId", policy.getId());
        value.put("policyVersion", policy.getPolicyVersion());
        value.put("version", policy.getVersion());
        value.put("effectiveFrom", policy.getEffectiveFrom());
        value.put("timezone", policy.getTimezone());
        value.put("deadlineMode", policy.getDeadlineMode());
        value.put("editWindowHours", policy.getEditWindowHours());
        value.put("requireHomeroomReview", policy.isRequireHomeroomReview());
        value.put("rubricJson", policy.getRubricJson());
        try {
            return (objectMapper == null ? new ObjectMapper().registerModule(new JavaTimeModule()) : objectMapper)
                    .writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không thể tạo snapshot policy", exception);
        }
    }

    private Long actor() {
        return AuditContext.currentUserId();
    }

    private AppException error(HttpStatus status, String message) {
        return new AppException(status, message);
    }
}
