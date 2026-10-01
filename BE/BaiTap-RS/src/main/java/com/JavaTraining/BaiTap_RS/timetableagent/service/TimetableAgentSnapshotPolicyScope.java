package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.HomeroomAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadEligibility;
import com.JavaTraining.BaiTap_RS.timetable.repository.TeacherLoadEligibilityRepository;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqCreateTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.service.TimetableAgentSnapshotPolicy.TimetableAgentHomeroomPeriod;
import com.JavaTraining.BaiTap_RS.timetableagent.service.TimetableAgentSnapshotPolicy.TimetableAgentLoadEligibility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimetableAgentSnapshotPolicyScope {

    private final TeacherLoadEligibilityRepository eligibilityRepository;
    private final HomeroomAssignmentRepository homeroomRepository;

    public List<TimetableAgentLoadEligibility> loadEligibilitySnapshot(Set<Long> teacherIds,
            ReqCreateTimetableAgentProposalDTO request) {
        List<TimetableAgentLoadEligibility> result = new ArrayList<>();
        LocalDate weekStart = request.validFrom().with(java.time.temporal.TemporalAdjusters.previousOrSame(
                java.time.DayOfWeek.MONDAY));
        while (!weekStart.isAfter(request.validTo())) {
            for (Long teacherId : teacherIds) {
                for (TeacherLoadEligibility eligibility : eligibilityRepository
                        .findActiveByTeacherIdAndDate(teacherId, weekStart)) {
                    result.add(new TimetableAgentLoadEligibility(teacherId, eligibility.getRuleCode(),
                            eligibility.getValidFrom(), eligibility.getValidTo(), weekStart));
                }
            }
            weekStart = weekStart.plusWeeks(1);
        }
        return result.stream().distinct().sorted(Comparator.comparing(TimetableAgentLoadEligibility::teacherId)
                .thenComparing(TimetableAgentLoadEligibility::weekStart)
                .thenComparing(TimetableAgentLoadEligibility::ruleCode)).toList();
    }

    public List<TimetableAgentHomeroomPeriod> homeroomSnapshot(Set<Long> teacherIds,
            ReqCreateTimetableAgentProposalDTO request) {
        List<TimetableAgentHomeroomPeriod> result = new ArrayList<>();
        LocalDate weekStart = request.validFrom().with(java.time.temporal.TemporalAdjusters.previousOrSame(
                java.time.DayOfWeek.MONDAY));
        while (!weekStart.isAfter(request.validTo())) {
            for (Long teacherId : teacherIds) {
                for (HomeroomAssignment assignment : homeroomRepository
                        .findAllByTeacherIdOrderByValidFromDesc(teacherId)) {
                    if (assignment.getStatus() == AssignmentStatus.ACTIVE
                            && !assignment.getValidFrom().isAfter(weekStart.plusDays(6))
                            && (assignment.getValidTo() == null || !assignment.getValidTo().isBefore(weekStart))) {
                        result.add(new TimetableAgentHomeroomPeriod(teacherId, assignment.getClassId(),
                                assignment.getValidFrom(), assignment.getValidTo(), weekStart));
                    }
                }
            }
            weekStart = weekStart.plusWeeks(1);
        }
        return result.stream().distinct().sorted(Comparator.comparing(TimetableAgentHomeroomPeriod::teacherId)
                .thenComparing(TimetableAgentHomeroomPeriod::weekStart)
                .thenComparing(TimetableAgentHomeroomPeriod::classId)).toList();
    }

}
