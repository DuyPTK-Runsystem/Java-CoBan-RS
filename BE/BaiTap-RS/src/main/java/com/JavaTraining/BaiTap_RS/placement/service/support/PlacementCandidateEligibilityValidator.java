package com.JavaTraining.BaiTap_RS.placement.service.support;

import java.util.Objects;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.repository.GradeLevelRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.requests.ReqCreatePlacementSessionDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSourceType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public final class PlacementCandidateEligibilityValidator {
    private final GradeLevelRepository gradeLevels;
    private final SchoolClassRepository classes;
    private final StudentYearEnrollmentRepository enrollments;

    public PlacementCandidateEligibilityValidator(GradeLevelRepository gradeLevels, SchoolClassRepository classes,
            StudentYearEnrollmentRepository enrollments) {
        this.gradeLevels = gradeLevels;
        this.classes = classes;
        this.enrollments = enrollments;
    }

    public void validate(ReqCreatePlacementSessionDTO.Candidate candidate, Optional<AcademicYear> previous,
            Long targetGradeId) {
        if (candidate.sourceType() == PlacementSourceType.CONTINUING) {
            validateContinuing(candidate, previous, targetGradeId);
            return;
        }
        validateEvidence(candidate);
        Optional<StudentYearEnrollment> prior = previous.flatMap(year ->
                enrollments.findByStudentIdAndAcademicYearId(candidate.studentId(), year.getId()));
        if (candidate.sourceType() == PlacementSourceType.NEW_ADMISSION && prior.isPresent()) {
            throw new AppException(HttpStatus.CONFLICT,
                    "NEW_ADMISSION không hợp lệ: học sinh đã có enrollment năm trước");
        }
        if (candidate.sourceType() == PlacementSourceType.REPEAT
                && (prior.isEmpty() || !isRepeatGrade(prior.get(), targetGradeId))) {
            throw new AppException(HttpStatus.CONFLICT,
                    "REPEAT phải có enrollment năm trước cùng khối đích");
        }
    }

    private void validateContinuing(ReqCreatePlacementSessionDTO.Candidate candidate,
            Optional<AcademicYear> previous, Long targetGradeId) {
        if (previous.isEmpty()
                || !hasValidPromotionPath(candidate.studentId(), previous.get().getId(), targetGradeId)) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Ứng viên CONTINUING không có enrollment năm trước hợp lệ để lên khối đích");
        }
    }

    private void validateEvidence(ReqCreatePlacementSessionDTO.Candidate candidate) {
        if (isBlank(candidate.eligibilityEvidence()) || isBlank(candidate.approvalReference())) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Ứng viên NEW_ADMISSION/REPEAT phải có minh chứng và phê duyệt");
        }
    }

    private boolean hasValidPromotionPath(Long studentId, Long previousYearId, Long targetGradeId) {
        return enrollments.findByStudentIdAndAcademicYearId(studentId, previousYearId)
                .flatMap(enrollment -> classes.findById(enrollment.getCurrentClassId()))
                .flatMap(previousClass -> gradeLevels.findById(previousClass.getGradeLevelId()))
                .map(grade -> Objects.equals(grade.getNextGradeId(), targetGradeId)).orElse(false);
    }

    private boolean isRepeatGrade(StudentYearEnrollment enrollment, Long targetGradeId) {
        return classes.findById(enrollment.getCurrentClassId())
                .map(previousClass -> Objects.equals(previousClass.getGradeLevelId(), targetGradeId)).orElse(false);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
