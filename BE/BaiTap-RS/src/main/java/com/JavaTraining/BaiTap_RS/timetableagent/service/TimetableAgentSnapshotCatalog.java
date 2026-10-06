package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqCreateTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentAssignmentOption;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimetableAgentSnapshotCatalog {

    private final ClassSubjectRepository classSubjectRepository;
    private final TimetableAgentSnapshotAssignments assignments;
    private final TimetableAgentSnapshotAssignmentLabels labels;

    public Catalog read(TimetableRevision revision, ReqCreateTimetableAgentProposalDTO request) {
        List<ClassSubject> classSubjects = classSubjectRepository.findAllBySemesterId(revision.getSemesterId())
                .stream()
                .filter(item -> request.classIds().contains(item.getClassId()))
                .filter(item -> item.getStatus() == ClassSubjectStatus.ACTIVE)
                .sorted(Comparator.comparing(ClassSubject::getClassId).thenComparing(ClassSubject::getSubjectId))
                .toList();
        Set<Long> foundClassIds = classSubjects.stream().map(ClassSubject::getClassId).collect(Collectors.toSet());
        if (!foundClassIds.containsAll(request.classIds())) {
            throw invalid("One or more selected classes are not active in the target semester.");
        }

        List<SubjectTeachingAssignment> activeAssignments = assignments.read(classSubjects, revision.getSemesterId(),
                request.validFrom(), request.validTo());
        Map<Long, Integer> demandMap = assignments.validateDemand(request, activeAssignments);
        TimetableAgentSnapshotAssignmentLabels.Labels projected = labels.read(classSubjects, activeAssignments,
                demandMap);
        Map<Long, ClassSubject> classSubjectById = classSubjects.stream()
                .collect(Collectors.toMap(ClassSubject::getId, item -> item));
        Set<Long> selectedAssignmentIds = activeAssignments.stream().map(SubjectTeachingAssignment::getId)
                .collect(Collectors.toSet());
        Set<Long> teacherIds = activeAssignments.stream().map(SubjectTeachingAssignment::getTeacherId)
                .collect(Collectors.toSet());
        return new Catalog(projected.assignmentOptions(), activeAssignments, classSubjectById, projected.subjectIds(),
                selectedAssignmentIds, teacherIds);
    }

    private AppException invalid(String message) {
        return new AppException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    public record Catalog(List<TimetableAgentAssignmentOption> assignmentOptions,
            List<SubjectTeachingAssignment> activeAssignments, Map<Long, ClassSubject> classSubjectById,
            Set<Long> subjectIds, Set<Long> selectedAssignmentIds, Set<Long> teacherIds) {
    }
}
