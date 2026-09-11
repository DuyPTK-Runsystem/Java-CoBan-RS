package com.JavaTraining.BaiTap_RS.timetable.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTeacherLoadDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadEligibility;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadPolicy;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetable.repository.TeacherLoadEligibilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeacherLoadEvaluator {

    private final TeacherRepository teacherRepository;
    private final TeacherLoadPolicyService policyService;
    private final TeacherLoadEligibilityRepository eligibilityRepository;
    private final HomeroomAssignmentRepository homeroomRepository;
    private final SubjectTeachingAssignmentRepository assignmentRepository;

    public List<ResTeacherLoadDTO> evaluateLoads(
            List<TimetableEntry> entries,
            LocalDate weekStart,
            LocalDate weekEnd,
            Long optionalPolicyId) {
        final LocalDate effectiveWeekStart = weekStart != null ? weekStart : LocalDate.now();
        final LocalDate effectiveWeekEnd = weekEnd != null ? weekEnd : effectiveWeekStart.plusDays(6);

        PolicyConfig config = resolvePolicyConfig();
        Map<Long, Integer> teacherPeriodCounts = countTeacherPeriods(entries, effectiveWeekStart, effectiveWeekEnd);

        Set<Long> teacherIds = teacherPeriodCounts.keySet();
        if (teacherIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, String> teacherNames = teacherRepository.findAllById(teacherIds)
                .stream().collect(Collectors.toMap(Teacher::getId, Teacher::getTeacherName));

        List<ResTeacherLoadDTO> results = new ArrayList<>();
        for (Long teacherId : teacherIds) {
            int assigned = teacherPeriodCounts.getOrDefault(teacherId, 0);
            String teacherName = teacherNames.getOrDefault(teacherId, "N/A");
            results.add(evaluateSingleTeacher(
                    teacherId, teacherName, assigned, config, effectiveWeekStart, effectiveWeekEnd));
        }

        return results;
    }

    private PolicyConfig resolvePolicyConfig() {
        Optional<TeacherLoadPolicy> policyOpt = policyService.getActivePolicy();
        TeacherLoadPolicy policy = policyOpt.orElse(null);
        int basePeriods = policy != null ? policy.getBasePeriods() : 19;
        int homeroomReduction = policy != null ? policy.getHomeroomReduction() : 4;
        int nursingReduction = policy != null ? policy.getNursingReduction() : 3;
        String policyVersion = policy != null ? policy.getVersion() : "CHƯA_CÓ";
        return new PolicyConfig(policy, basePeriods, homeroomReduction, nursingReduction, policyVersion);
    }

    private Map<Long, Integer> countTeacherPeriods(
            List<TimetableEntry> entries,
            LocalDate weekStart,
            LocalDate weekEnd) {
        Map<Long, SubjectTeachingAssignment> assignmentMap = assignmentRepository.findAllById(
                entries.stream().map(TimetableEntry::getAssignmentId).distinct().toList())
                .stream().collect(Collectors.toMap(SubjectTeachingAssignment::getId, a -> a));

        return entries.stream()
                .filter(e -> !e.getValidFrom().isAfter(weekEnd) && !e.getValidTo().isBefore(weekStart))
                .map(e -> assignmentMap.get(e.getAssignmentId()))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        SubjectTeachingAssignment::getTeacherId,
                        Collectors.summingInt(a -> 1)));
    }

    private ResTeacherLoadDTO evaluateSingleTeacher(
            Long teacherId,
            String teacherName,
            int assigned,
            PolicyConfig config,
            LocalDate weekStart,
            LocalDate weekEnd) {
        int reductions = computeReductions(teacherId, config, weekStart, weekEnd);
        int target = Math.max(0, config.basePeriods() - reductions);
        int diff = assigned - target;
        String evalStatus = resolveEvalStatus(config.policy(), diff);

        return new ResTeacherLoadDTO(
                teacherId,
                teacherName,
                weekStart,
                assigned,
                config.basePeriods(),
                reductions,
                target,
                diff,
                config.policyVersion(),
                evalStatus);
    }

    private int computeReductions(
            Long teacherId,
            PolicyConfig config,
            LocalDate weekStart,
            LocalDate weekEnd) {
        int reductions = 0;
        boolean isHomeroom = homeroomRepository.findAllByTeacherIdOrderByValidFromDesc(teacherId).stream()
                .anyMatch(h -> h.getStatus() == AssignmentStatus.ACTIVE
                        && !h.getValidFrom().isAfter(weekEnd)
                        && (h.getValidTo() == null || !h.getValidTo().isBefore(weekStart)));
        if (isHomeroom) {
            reductions += config.homeroomReduction();
        }

        List<TeacherLoadEligibility> eligibilities = eligibilityRepository.findActiveByTeacherIdAndDate(teacherId,
                weekStart);
        boolean isNursing = eligibilities.stream()
                .anyMatch(e -> "NURSING_CHILD_UNDER_12M".equalsIgnoreCase(e.getRuleCode()));
        if (isNursing) {
            reductions += config.nursingReduction();
        }
        return reductions;
    }

    private String resolveEvalStatus(TeacherLoadPolicy policy, int diff) {
        if (policy == null) {
            return "LOAD_UNDETERMINED";
        }
        if (diff > 0) {
            return "LOAD_ABOVE_TARGET";
        }
        if (diff < 0) {
            return "LOAD_BELOW_TARGET";
        }
        return "NORMAL";
    }

    private record PolicyConfig(
            TeacherLoadPolicy policy,
            int basePeriods,
            int homeroomReduction,
            int nursingReduction,
            String policyVersion) {
    }
}
