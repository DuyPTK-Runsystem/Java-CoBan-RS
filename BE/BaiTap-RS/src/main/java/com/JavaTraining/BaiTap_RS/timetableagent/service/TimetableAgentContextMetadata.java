package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentSnapshotEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimetableAgentContextMetadata {

    private final SubjectTeachingAssignmentRepository assignmentRepository;
    private final ClassSubjectRepository classSubjectRepository;

    public ContextAssignments read(List<TimetableAgentSnapshotEntry> currentEntries,
            List<TimetableAgentSnapshotEntry> publishedEntries, Set<Long> selectedAssignmentIds) {
        Set<Long> contextAssignmentIds = new HashSet<>();
        currentEntries.stream().map(TimetableAgentSnapshotEntry::assignmentId).forEach(contextAssignmentIds::add);
        publishedEntries.stream().map(TimetableAgentSnapshotEntry::assignmentId).forEach(contextAssignmentIds::add);
        contextAssignmentIds.removeAll(selectedAssignmentIds);
        if (contextAssignmentIds.isEmpty()) {
            return new ContextAssignments(Map.of(), Map.of());
        }

        List<Long> sortedIds = contextAssignmentIds.stream().sorted().toList();
        List<SubjectTeachingAssignment> assignments = assignmentRepository.findAllById(sortedIds).stream()
                .filter(assignment -> assignment.getId() != null)
                .sorted(Comparator.comparing(SubjectTeachingAssignment::getId)).toList();
        Map<Long, Long> teacherIds = assignments.stream()
                .filter(assignment -> assignment.getTeacherId() != null)
                .collect(Collectors.toMap(SubjectTeachingAssignment::getId, SubjectTeachingAssignment::getTeacherId,
                        (first, ignored) -> first, TreeMap::new));

        List<Long> classSubjectIds = assignments.stream().map(SubjectTeachingAssignment::getClassSubjectId)
                .filter(id -> id != null).distinct().sorted().toList();
        Map<Long, Long> classIds = new TreeMap<>();
        if (!classSubjectIds.isEmpty()) {
            Map<Long, ClassSubject> classSubjects = classSubjectRepository.findAllById(classSubjectIds).stream()
                    .filter(classSubject -> classSubject.getId() != null && classSubject.getClassId() != null)
                    .collect(Collectors.toMap(ClassSubject::getId, classSubject -> classSubject));
            assignments.stream().filter(assignment -> assignment.getClassSubjectId() != null)
                    .forEach(assignment -> {
                        ClassSubject classSubject = classSubjects.get(assignment.getClassSubjectId());
                        if (classSubject != null) {
                            classIds.put(assignment.getId(), classSubject.getClassId());
                        }
                    });
        }
        return new ContextAssignments(teacherIds, classIds);
    }

    public record ContextAssignments(Map<Long, Long> teacherIds, Map<Long, Long> classIds) {
    }
}
