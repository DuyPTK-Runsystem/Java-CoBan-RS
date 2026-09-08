package com.JavaTraining.BaiTap_RS.academic.service;

import java.util.List;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicability;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectApplicabilityClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import org.springframework.stereotype.Service;

@Service
public class ClassSubjectApplicabilityProvisioningService {

    private final ClassSubjectRepository classSubjectRepository;
    private final ClassSubjectApplicabilityClassRepository classRepository;

    public ClassSubjectApplicabilityProvisioningService(
            ClassSubjectRepository classSubjectRepository,
            ClassSubjectApplicabilityClassRepository classRepository) {
        this.classSubjectRepository = classSubjectRepository;
        this.classRepository = classRepository;
    }

    public void createForGradeApplicability(SubjectApplicability applicability, Semester semester) {
        if (applicability.getScopeType() != ApplicationScope.GRADE) {
            return;
        }
        List<SchoolClass> classes = classRepository
                .findAllByAcademicYearIdAndGradeLevelIdOrderByClassCodeAsc(
                        semester.getAcademicYearId(), applicability.getGradeLevelId());
        if (classes.isEmpty()) {
            return;
        }
        Set<Long> classIds = classes.stream().map(SchoolClass::getId).collect(java.util.stream.Collectors.toSet());
        Set<Long> configuredClassIds = classSubjectRepository
                .findAllBySubjectIdAndSemesterIdAndClassIdIn(
                        applicability.getSubjectId(), applicability.getSemesterId(), classIds)
                .stream()
                .map(ClassSubject::getClassId)
                .collect(java.util.stream.Collectors.toSet());
        List<ClassSubject> newClassSubjects = classes.stream()
                .filter(schoolClass -> !configuredClassIds.contains(schoolClass.getId()))
                .map(schoolClass -> new ClassSubject(
                        schoolClass.getId(), applicability.getSubjectId(), applicability.getSemesterId(),
                        ClassSubjectStatus.ACTIVE))
                .toList();
        if (!newClassSubjects.isEmpty()) {
            classSubjectRepository.saveAll(newClassSubjects);
        }
    }

    public boolean hasConfiguredTarget(
            Long subjectId,
            Long semesterId,
            ApplicationScope scopeType,
            Long gradeLevelId,
            Long classId) {
        return classSubjectRepository.existsByApplicabilityTarget(
                subjectId, semesterId, scopeType, gradeLevelId, classId);
    }
}
