package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadPolicy;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadPolicyStatus;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.repository.TeacherLoadPolicyRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TeacherLoadRuleRepository;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqCreateTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentSnapshotEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimetableAgentSnapshotPolicy {

    private final TeacherLoadPolicyRepository loadPolicyRepository;
    private final TeacherLoadRuleRepository loadRuleRepository;
    private final TimetableAgentSnapshotPolicyScope scope;
    private final TimetableAgentSnapshotLoads loads;

    public PolicyData read(TimetableRevision revision, ReqCreateTimetableAgentProposalDTO request,
            TeacherLoadPolicy loadPolicy, Set<Long> teacherIds, TimetableAgentSnapshotEntries.EntryData entries) {
        List<TimetableAgentSnapshotEntry> currentEntries = entries.current();
        List<TimetableAgentSnapshotEntry> publishedContext = entries.published();
        List<TimetableAgentLoadRule> policyRules = loadRuleRepository
                .findAllByPolicyIdOrderByIdAsc(loadPolicy.getId()).stream()
                .map(rule -> new TimetableAgentLoadRule(rule.getRuleCode(), rule.getTriggerType().name(),
                        rule.getReductionPeriods(), rule.isActive()))
                .toList();
        if (policyRules.isEmpty()) {
            policyRules = List.of(
                    new TimetableAgentLoadRule("HOMEROOM", "HOMEROOM", loadPolicy.getHomeroomReduction(), true),
                    new TimetableAgentLoadRule("NURSING_CHILD_UNDER_12M", "ELIGIBILITY",
                            loadPolicy.getNursingReduction(), true));
        }
        List<TimetableAgentLoadEligibility> loadEligibilities = scope.loadEligibilitySnapshot(teacherIds, request);
        List<TimetableAgentHomeroomPeriod> homeroomPeriods = scope.homeroomSnapshot(teacherIds, request);
        List<TimetableAgentWeeklyLoad> computedLoads = loads.computedLoadSnapshot(revision, request,
                currentEntries, publishedContext);
        List<TimetableAgentWeeklyTarget> weeklyTargets = computedLoads.stream()
                .map(load -> new TimetableAgentWeeklyTarget(load.teacherId(), load.weekStart(), load.targetPeriods()))
                .toList();

        return new PolicyData(policySnapshot(loadPolicy), policyRules, weeklyTargets, loadEligibilities,
                homeroomPeriods, computedLoads);
    }

    public TeacherLoadPolicy requireCurrentPinnedPolicy(TimetableRevision revision) {
        if (revision.getPolicyId() == null) {
            throw invalid("The draft does not have a confirmed teacher-load policy.");
        }
        TeacherLoadPolicy pinnedPolicy = loadPolicyRepository.findById(revision.getPolicyId())
                .orElseThrow(() -> invalid("The draft teacher-load policy is unavailable."));
        TeacherLoadPolicy activePolicy = loadPolicyRepository
                .findFirstByStatusOrderByEffectiveFromDesc(TeacherLoadPolicyStatus.ACTIVE)
                .orElseThrow(() -> invalid("There is no active teacher-load policy."));
        if (!Objects.equals(activePolicy.getId(), pinnedPolicy.getId())) {
            throw invalid("The draft policy is no longer active; refresh the draft before generating a proposal.");
        }
        return pinnedPolicy;
    }

    private TeacherLoadPolicySnapshot policySnapshot(TeacherLoadPolicy policy) {
        return new TeacherLoadPolicySnapshot(policy.getId(), policy.getVersion(),
                policy.getEffectiveFrom(), policy.getEffectiveTo(), policy.getBasePeriods(),
                policy.getHomeroomReduction(), policy.getNursingReduction());
    }

    private AppException invalid(String message) {
        return new AppException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    public record TeacherLoadPolicySnapshot(Long policyId, String version,
            LocalDate effectiveFrom, LocalDate effectiveTo, Integer basePeriods,
            Integer homeroomReduction, Integer nursingReduction) {
    }

    public record TimetableAgentLoadRule(String ruleCode, String triggerType, Integer reductionPeriods,
            boolean active) {
    }

    public record TimetableAgentLoadEligibility(Long teacherId, String ruleCode, LocalDate validFrom,
            LocalDate validTo, LocalDate weekStart) {
    }

    public record TimetableAgentHomeroomPeriod(Long teacherId, Long classId, LocalDate validFrom,
            LocalDate validTo, LocalDate weekStart) {
    }

    public record TimetableAgentWeeklyLoad(Long teacherId, LocalDate weekStart, int assignedPeriods,
            int targetPeriods, int basePeriods, int reductionPeriods, String evaluationStatus) {
    }

    public record TimetableAgentWeeklyTarget(Long teacherId, LocalDate weekStart, int targetPeriods) {
    }

    public record PolicyData(TeacherLoadPolicySnapshot policy, List<TimetableAgentLoadRule> policyRules,
            List<TimetableAgentWeeklyTarget> weeklyTargets, List<TimetableAgentLoadEligibility> eligibility,
            List<TimetableAgentHomeroomPeriod> homeroom, List<TimetableAgentWeeklyLoad> loads) {
    }
}
