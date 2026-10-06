package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqCreateTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqTimetableAgentDemandDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimetableAgentSnapshotAssignments {

    private final SubjectTeachingAssignmentRepository assignmentRepository;

    public List<SubjectTeachingAssignment> read(List<ClassSubject> classSubjects, Long semesterId,
            LocalDate from, LocalDate to) {
        Set<Long> classIds = classSubjects.stream().map(ClassSubject::getClassId).collect(Collectors.toSet());
        Set<Long> activeClassSubjectIds = classSubjects.stream().map(ClassSubject::getId).collect(Collectors.toSet());
        return classIds.stream()
                .flatMap(classId -> assignmentRepository.findAllByClassIdAndSemesterIdOrderByValidFromDesc(classId,
                        semesterId).stream())
                .filter(item -> activeClassSubjectIds.contains(item.getClassSubjectId()))
                .filter(item -> item.getStatus() == AssignmentStatus.ACTIVE)
                .filter(item -> !item.getValidFrom().isAfter(to)
                        && (item.getValidTo() == null || !item.getValidTo().isBefore(from)))
                .collect(Collectors.toMap(SubjectTeachingAssignment::getId, item -> item, (first, second) -> first))
                .values().stream().sorted(Comparator.comparing(SubjectTeachingAssignment::getId)).toList();
    }

    public Map<Long, Integer> validateDemand(ReqCreateTimetableAgentProposalDTO request,
            List<SubjectTeachingAssignment> assignments) {
        Map<Long, Integer> result = new HashMap<>();
        for (ReqTimetableAgentDemandDTO demand : request.demands()) {
            if (result.putIfAbsent(demand.assignmentId(), demand.periodsPerWeek()) != null) {
                throw invalid("Weekly demand contains a duplicate assignment.");
            }
        }
        Set<Long> assignmentIds = assignments.stream().map(SubjectTeachingAssignment::getId)
                .collect(Collectors.toSet());
        if (!result.keySet().equals(assignmentIds)) {
            throw invalid("Weekly demand must confirm every active assignment in the selected classes.");
        }
        return result;
    }

    private AppException invalid(String message) {
        return new AppException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

}
