package com.JavaTraining.BaiTap_RS.assignment.service;

import java.time.LocalDate;
import java.util.List;

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
public class DemoAssignmentSeeder {

    private static final LocalDate HOMEROOM_START = LocalDate.of(2026, 9, 1);

    private final HomeroomAssignmentRepository homeroomAssignmentRepository;
    private final SubjectTeachingAssignmentRepository teachingAssignmentRepository;

    public DemoAssignmentSeeder(
            HomeroomAssignmentRepository homeroomAssignmentRepository,
            SubjectTeachingAssignmentRepository teachingAssignmentRepository) {
        this.homeroomAssignmentRepository = homeroomAssignmentRepository;
        this.teachingAssignmentRepository = teachingAssignmentRepository;
    }

    public void seed(
            List<SchoolClass> classes,
            List<Semester> semesters,
            List<ClassSubject> classSubjects,
            List<Teacher> teachers,
            Long assignedBy) {
        seedHomerooms(classes, teachers, assignedBy);
        seedSubjectAssignments(classSubjects, semesters, teachers, assignedBy);
    }

    private void seedHomerooms(
            List<SchoolClass> classes,
            List<Teacher> teachers,
            Long assignedBy) {
        for (int index = 0; index < classes.size(); index++) {
            SchoolClass schoolClass = classes.get(index);
            if (homeroomAssignmentRepository
                    .findFirstByClassIdAndStatus(schoolClass.getId(), AssignmentStatus.ACTIVE)
                    .isEmpty()) {
                homeroomAssignmentRepository.save(
                        createHomeroomAssignment(schoolClass, teachers.get(index), assignedBy));
            }
        }
    }

    private void seedSubjectAssignments(
            List<ClassSubject> classSubjects,
            List<Semester> semesters,
            List<Teacher> teachers,
            Long assignedBy) {
        int teacherIndex = 0;
        for (ClassSubject classSubject : classSubjects) {
            if (teachingAssignmentRepository
                    .findFirstByClassSubjectIdAndStatus(
                            classSubject.getId(), AssignmentStatus.ACTIVE)
                    .isEmpty()) {
                Semester semester = semesters.stream()
                        .filter(item -> item.getId().equals(classSubject.getSemesterId()))
                        .findFirst()
                        .orElseThrow();
                teachingAssignmentRepository.save(createTeachingAssignment(
                        classSubject, teachers.get(teacherIndex % teachers.size()), semester, assignedBy));
            }
            teacherIndex++;
        }
    }

    private HomeroomAssignment createHomeroomAssignment(
            SchoolClass schoolClass,
            Teacher teacher,
            Long assignedBy) {
        return new HomeroomAssignment(
                schoolClass.getId(),
                teacher.getId(),
                HOMEROOM_START,
                null,
                AssignmentStatus.ACTIVE,
                assignedBy);
    }

    private SubjectTeachingAssignment createTeachingAssignment(
            ClassSubject classSubject,
            Teacher teacher,
            Semester semester,
            Long assignedBy) {
        return new SubjectTeachingAssignment(
                classSubject.getId(),
                teacher.getId(),
                semester.getStartDate(),
                null,
                AssignmentStatus.ACTIVE,
                assignedBy);
    }
}
