package com.JavaTraining.BaiTap_RS.academic.service;

import java.util.ArrayList;
import java.util.List;
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
final class DemoAcademicApplicabilityStore {

    private final SubjectApplicabilityRepository applicabilityRepository;
    private final ClassSubjectRepository classSubjectRepository;

    /* default */ DemoAcademicApplicabilityStore(
            SubjectApplicabilityRepository applicabilityRepository,
            ClassSubjectRepository classSubjectRepository) {
        this.applicabilityRepository = applicabilityRepository;
        this.classSubjectRepository = classSubjectRepository;
    }

    /* default */ List<SubjectApplicability> loadExistingApplicability() {
        return new ArrayList<>(applicabilityRepository.findAll());
    }

    /* default */ void saveApplicability(
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

    /* default */ ClassSubject findOrCreateClassSubject(
            SchoolClass schoolClass,
            Semester semester,
            Subject subject) {
        return classSubjectRepository.findAllByClassIdAndSemesterIdOrderBySubjectIdAsc(
                        schoolClass.getId(), semester.getId()).stream()
                .filter(existing -> Objects.equals(existing.getSubjectId(), subject.getId()))
                .findFirst()
                .orElseGet(() -> classSubjectRepository.save(createClassSubject(
                        schoolClass, subject, semester)));
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
}
