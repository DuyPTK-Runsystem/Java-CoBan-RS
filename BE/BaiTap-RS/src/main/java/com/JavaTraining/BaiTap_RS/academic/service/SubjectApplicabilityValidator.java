package com.JavaTraining.BaiTap_RS.academic.service;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicabilityStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.academic.repository.GradeLevelRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectApplicabilityRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SubjectApplicabilityValidator {

    private final SubjectApplicabilityRepository applicabilityRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolClassRepository schoolClassRepository;

    public SubjectApplicabilityValidator(
            SubjectApplicabilityRepository applicabilityRepository,
            GradeLevelRepository gradeLevelRepository,
            SchoolClassRepository schoolClassRepository) {
        this.applicabilityRepository = applicabilityRepository;
        this.gradeLevelRepository = gradeLevelRepository;
        this.schoolClassRepository = schoolClassRepository;
    }

    public void validateTarget(
            Subject subject,
            Semester semester,
            Long semesterId,
            ApplicationScope scopeType,
            Long gradeLevelId,
            Long classId,
            Long currentId,
            SubjectApplicabilityStatus requestedStatus) {
        if (subject.getApplicationScope() != scopeType) {
            throw conflict("Scope áp dụng không khớp cấu hình môn học");
        }
        ensureSemesterMutable(semester);
        if (scopeType == ApplicationScope.GRADE) {
            validateGradeTarget(subject, semesterId, gradeLevelId, classId, currentId);
        } else {
            validateClassTarget(subject, semester, semesterId, gradeLevelId, classId, currentId);
        }
        if (requestedStatus == SubjectApplicabilityStatus.ACTIVE
                && subject.getSubjectType() == SubjectType.SKILL
                && activeApplicabilityCount(subject.getId(), currentId) > 0) {
            throw conflict("Môn SKILL chỉ được cấu hình trong một học kỳ");
        }
    }

    public void ensureSemesterMutable(Semester semester) {
        if (semester.getStatus() == SemesterStatus.CLOSED) {
            throw conflict("Không thể thay đổi applicability của học kỳ đã CLOSED");
        }
    }

    private void validateGradeTarget(
            Subject subject,
            Long semesterId,
            Long gradeLevelId,
            Long classId,
            Long currentId) {
        if (gradeLevelId == null || classId != null) {
            throw conflict("Scope GRADE phải có gradeLevelId và không có classId");
        }
        if (!gradeLevelRepository.existsById(gradeLevelId)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khối");
        }
        boolean duplicate = currentId == null
                ? applicabilityRepository.existsBySubjectIdAndSemesterIdAndScopeTypeAndGradeLevelId(
                        subject.getId(), semesterId, ApplicationScope.GRADE, gradeLevelId)
                : applicabilityRepository.existsBySubjectIdAndSemesterIdAndScopeTypeAndGradeLevelIdAndIdNot(
                        subject.getId(), semesterId, ApplicationScope.GRADE, gradeLevelId, currentId);
        if (duplicate) {
            throw conflict("Cấu hình môn theo khối đã tồn tại");
        }
    }

    private void validateClassTarget(
            Subject subject,
            Semester semester,
            Long semesterId,
            Long gradeLevelId,
            Long classId,
            Long currentId) {
        if (classId == null || gradeLevelId != null) {
            throw conflict("Scope CLASS phải có classId và không có gradeLevelId");
        }
        schoolClassRepository.findById(classId)
                .filter(schoolClass -> schoolClass.getAcademicYearId().equals(semester.getAcademicYearId()))
                .orElseThrow(() -> conflict("Lớp không thuộc năm học của học kỳ"));
        boolean duplicate = currentId == null
                ? applicabilityRepository.existsBySubjectIdAndSemesterIdAndScopeTypeAndClassId(
                        subject.getId(), semesterId, ApplicationScope.CLASS, classId)
                : applicabilityRepository.existsBySubjectIdAndSemesterIdAndScopeTypeAndClassIdAndIdNot(
                        subject.getId(), semesterId, ApplicationScope.CLASS, classId, currentId);
        if (duplicate) {
            throw conflict("Cấu hình môn theo lớp đã tồn tại");
        }
    }

    private long activeApplicabilityCount(Long subjectId, Long currentId) {
        return currentId == null
                ? applicabilityRepository.countBySubjectIdAndStatus(subjectId, SubjectApplicabilityStatus.ACTIVE)
                : applicabilityRepository.countBySubjectIdAndStatusAndIdNot(
                        subjectId, SubjectApplicabilityStatus.ACTIVE, currentId);
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}
