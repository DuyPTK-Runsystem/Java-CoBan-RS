package com.JavaTraining.BaiTap_RS.academic.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.GradeLevel;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicability;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicabilityStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectApplicabilityRepository;
import org.springframework.stereotype.Component;

@Component
public class DemoAcademicApplicabilitySeeder {

    private final SubjectApplicabilityRepository applicabilityRepository;
    private final ClassSubjectRepository classSubjectRepository;

    public DemoAcademicApplicabilitySeeder(
            SubjectApplicabilityRepository applicabilityRepository,
            ClassSubjectRepository classSubjectRepository) {
        this.applicabilityRepository = applicabilityRepository;
        this.classSubjectRepository = classSubjectRepository;
    }

    public void seedApplicability(
            Map<String, Subject> subjects,
            List<Semester> semesters,
            Map<Integer, GradeLevel> grades) {
        List<SubjectApplicability> existing = new ArrayList<>(applicabilityRepository.findAll());
        for (Subject subject : subjects.values()) {
            seedSubjectApplicability(subject, semesters, grades, existing);
        }
    }

    public List<ClassSubject> seedClassSubjects(
            List<SchoolClass> classes,
            List<Semester> semesters,
            Map<String, Subject> subjects) {
        List<ClassSubject> classSubjects = new ArrayList<>();
        for (SchoolClass schoolClass : classes) {
            seedClassSubjectsForClass(schoolClass, semesters, subjects, classSubjects);
        }
        return classSubjects;
    }

    private void seedSubjectApplicability(
            Subject subject,
            List<Semester> semesters,
            Map<Integer, GradeLevel> grades,
            List<SubjectApplicability> existing) {
        for (Semester semester : semesters) {
            for (Map.Entry<Integer, GradeLevel> grade : grades.entrySet()) {
                if (isApplicable(subject, grade.getKey(), semester.getCode())) {
                    saveApplicability(subject, semester, grade.getValue(), existing);
                }
            }
        }
    }

    private void saveApplicability(
            Subject subject,
            Semester semester,
            GradeLevel grade,
            List<SubjectApplicability> existing) {
        SubjectApplicability applicability = findApplicability(existing, subject, semester, grade);
        if (applicability == null) {
            applicability = createApplicability(subject, semester, grade);
            existing.add(applicability);
        }
        applicability.setStatus(SubjectApplicabilityStatus.ACTIVE);
        applicabilityRepository.save(applicability);
    }

    private void seedClassSubjectsForClass(
            SchoolClass schoolClass,
            List<Semester> semesters,
            Map<String, Subject> subjects,
            List<ClassSubject> classSubjects) {
        int grade = Integer.parseInt(schoolClass.getClassCode().substring(0, 1));
        for (Semester semester : semesters) {
            for (Subject subject : subjects.values()) {
                if (isApplicable(subject, grade, semester.getCode())) {
                    classSubjects.add(findOrCreateClassSubject(schoolClass, semester, subject));
                }
            }
        }
    }

    private ClassSubject findOrCreateClassSubject(
            SchoolClass schoolClass,
            Semester semester,
            Subject subject) {
        return classSubjectRepository.findAllByClassIdAndSemesterIdOrderBySubjectIdAsc(
                        schoolClass.getId(), semester.getId()).stream()
                .filter(existing -> Objects.equals(existing.getSubjectId(), subject.getId()))
                .findFirst()
                .orElseGet(() -> classSubjectRepository.save(createClassSubject(schoolClass, subject, semester)));
    }

    private SubjectApplicability findApplicability(
            List<SubjectApplicability> existing,
            Subject subject,
            Semester semester,
            GradeLevel grade) {
        return existing.stream()
                .filter(item -> Objects.equals(item.getSubjectId(), subject.getId()))
                .filter(item -> Objects.equals(item.getSemesterId(), semester.getId()))
                .filter(item -> item.getScopeType() == ApplicationScope.GRADE)
                .filter(item -> Objects.equals(item.getGradeLevelId(), grade.getId()))
                .findFirst()
                .orElse(null);
    }

    private SubjectApplicability createApplicability(
            Subject subject,
            Semester semester,
            GradeLevel grade) {
        return new SubjectApplicability(
                subject.getId(),
                semester.getId(),
                ApplicationScope.GRADE,
                grade.getId(),
                null,
                SubjectApplicabilityStatus.ACTIVE);
    }

    private ClassSubject createClassSubject(
            SchoolClass schoolClass,
            Subject subject,
            Semester semester) {
        return new ClassSubject(
                schoolClass.getId(),
                subject.getId(),
                semester.getId(),
                ClassSubjectStatus.ACTIVE);
    }

    private boolean isApplicable(Subject subject, int grade, String semesterCode) {
        if ("NGHE_DIEN".equals(subject.getCode()) || "NGHE_NONG_NGHIEP".equals(subject.getCode())) {
            return grade >= 8 && "HK2".equals(semesterCode);
        }
        return !("CONG_NGHE".equals(subject.getCode()) && grade == 9 && "HK2".equals(semesterCode));
    }
}
