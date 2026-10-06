package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentAssignmentOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimetableAgentSnapshotAssignmentLabels {

    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;

    public Labels read(List<ClassSubject> classSubjects, List<SubjectTeachingAssignment> activeAssignments,
            Map<Long, Integer> demandMap) {
        Set<Long> classIds = classSubjects.stream().map(ClassSubject::getClassId).collect(Collectors.toSet());
        Map<Long, SchoolClass> classMap = schoolClassRepository.findAllById(classIds).stream()
                .collect(Collectors.toMap(SchoolClass::getId, item -> item));
        Map<Long, Subject> subjectMap = subjectRepository.findAllById(
                classSubjects.stream().map(ClassSubject::getSubjectId).distinct().toList()).stream()
                .collect(Collectors.toMap(Subject::getId, item -> item));

        Map<Long, Teacher> teachers = teacherRepository.findAllById(
                activeAssignments.stream().map(SubjectTeachingAssignment::getTeacherId).distinct().toList()).stream()
                .collect(Collectors.toMap(Teacher::getId, item -> item));

        Map<Long, ClassSubject> classSubjectById = classSubjects.stream()
                .collect(Collectors.toMap(ClassSubject::getId, item -> item));
        List<TimetableAgentAssignmentOption> assignmentOptions = activeAssignments.stream()
                .map(assignment -> assignmentOption(assignment, classSubjectById, classMap, subjectMap,
                        teachers, demandMap))
                .sorted(Comparator.comparing(TimetableAgentAssignmentOption::assignmentId))
                .toList();

        return new Labels(assignmentOptions, subjectMap.keySet());
    }

    private TimetableAgentAssignmentOption assignmentOption(SubjectTeachingAssignment assignment,
            Map<Long, ClassSubject> classSubjects, Map<Long, SchoolClass> classes, Map<Long, Subject> subjects,
            Map<Long, Teacher> teachers, Map<Long, Integer> demands) {
        ClassSubject classSubject = classSubjects.get(assignment.getClassSubjectId());
        SchoolClass schoolClass = classSubject == null ? null : classes.get(classSubject.getClassId());
        Subject subject = classSubject == null ? null : subjects.get(classSubject.getSubjectId());
        Teacher teacher = teachers.get(assignment.getTeacherId());
        return new TimetableAgentAssignmentOption(
                assignment.getId(),
                classSubject == null ? null : classSubject.getClassId(),
                schoolClass == null ? null : schoolClass.getClassName(),
                classSubject == null ? null : classSubject.getSubjectId(),
                subject == null ? null : subject.getName(),
                assignment.getTeacherId(),
                teacher == null ? null : teacher.getTeacherName(),
                assignment.getValidFrom(), assignment.getValidTo(), demands.get(assignment.getId()));
    }

    public record Labels(List<TimetableAgentAssignmentOption> assignmentOptions, Set<Long> subjectIds) {
    }
}
