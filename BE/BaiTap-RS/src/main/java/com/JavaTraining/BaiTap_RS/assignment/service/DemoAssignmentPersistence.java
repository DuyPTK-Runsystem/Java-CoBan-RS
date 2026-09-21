package com.JavaTraining.BaiTap_RS.assignment.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.HomeroomAssignment;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import org.springframework.stereotype.Component;

@Component
final class DemoAssignmentPersistence {

    private static final LocalDate HOMEROOM_START = LocalDate.of(2026, 9, 1);
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;
    private final SubjectTeachingAssignmentRepository teachingAssignmentRepository;

    /* default */ DemoAssignmentPersistence(
            HomeroomAssignmentRepository homeroomAssignmentRepository,
            SubjectTeachingAssignmentRepository teachingAssignmentRepository) {
        this.homeroomAssignmentRepository = homeroomAssignmentRepository;
        this.teachingAssignmentRepository = teachingAssignmentRepository;
    }

    /* default */ void seedHomerooms(
            Map<String, SchoolClass> classesByCode,
            Map<String, Teacher> teachersByCode,
            Long assignedBy) {
        for (int index = 0; index < DemoAssignmentFixture.EXPECTED_CLASS_CODES.size(); index++) {
            SchoolClass schoolClass = classesByCode.get(DemoAssignmentFixture.EXPECTED_CLASS_CODES.get(index));
            Teacher teacher = teachersByCode.get(DemoAssignmentFixture.EXPECTED_TEACHER_CODES.get(index));
            Optional<HomeroomAssignment> existing = homeroomAssignmentRepository
                    .findFirstByClassIdAndStatus(schoolClass.getId(), AssignmentStatus.ACTIVE);
            if (existing.isPresent() && !existing.get().getTeacherId().equals(teacher.getId())) {
                throw new IllegalStateException("Active homeroom assignment conflicts for class "
                        + schoolClass.getClassCode() + ": expected " + teacher.getTeacherCode()
                        + ", found teacher id " + existing.get().getTeacherId());
            }
            if (existing.isEmpty()) {
                homeroomAssignmentRepository.save(createHomeroomAssignment(schoolClass, teacher, assignedBy));
            }
        }
    }

    /* default */ void persistAssignments(
            List<DemoAssignmentSeeder.PlannedAssignment> plan,
            Map<String, Teacher> teachersByCode,
            Map<String, Semester> semestersByCode,
            Long assignedBy) {
        for (DemoAssignmentSeeder.PlannedAssignment planned : plan) {
            persistAssignment(planned, teachersByCode, semestersByCode, assignedBy);
        }
    }

    private void persistAssignment(
            DemoAssignmentSeeder.PlannedAssignment planned,
            Map<String, Teacher> teachersByCode,
            Map<String, Semester> semestersByCode,
            Long assignedBy) {
        DemoAssignmentSeeder.WorkItem item = planned.item();
        Teacher teacher = teachersByCode.get(planned.teacherCode());
        List<SubjectTeachingAssignment> active = teachingAssignmentRepository
                .findAllByClassSubjectIdOrderByValidFromDesc(item.classSubject().getId()).stream()
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.ACTIVE)
                .toList();
        if (active.size() > 1) {
            throw new IllegalStateException("Duplicate active teaching assignments for class "
                    + item.classCode() + ", semester " + item.semesterCode()
                    + ", subject " + item.subjectCode());
        }
        if (active.isEmpty()) {
            teachingAssignmentRepository.save(createTeachingAssignment(
                    item.classSubject(), teacher, semestersByCode.get(item.semesterCode()), assignedBy));
        } else if (!active.get(0).getTeacherId().equals(teacher.getId())) {
            throw new IllegalStateException("Active teaching assignment conflicts for class "
                    + item.classCode() + ", semester " + item.semesterCode()
                    + ", subject " + item.subjectCode() + ": expected " + planned.teacherCode()
                    + ", found teacher id " + active.get(0).getTeacherId());
        }
    }

    private HomeroomAssignment createHomeroomAssignment(
            SchoolClass schoolClass,
            Teacher teacher,
            Long assignedBy) {
        return new HomeroomAssignment(
                schoolClass.getId(), teacher.getId(), HOMEROOM_START, null, AssignmentStatus.ACTIVE, assignedBy);
    }

    private SubjectTeachingAssignment createTeachingAssignment(
            ClassSubject classSubject,
            Teacher teacher,
            Semester semester,
            Long assignedBy) {
        return new SubjectTeachingAssignment(
                classSubject.getId(), teacher.getId(), semester.getStartDate(), null,
                AssignmentStatus.ACTIVE, assignedBy);
    }
}
