package com.JavaTraining.BaiTap_RS.academic.service;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateSubjectApplicabilityDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSubjectApplicabilityDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicability;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicabilityStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.academic.repository.GradeLevelRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectApplicabilityRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectApplicabilityService {

    private final SubjectRepository subjectRepository;
    private final SubjectApplicabilityRepository applicabilityRepository;
    private final SemesterRepository semesterRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolClassRepository schoolClassRepository;

    public SubjectApplicabilityService(
            SubjectRepository subjectRepository,
            SubjectApplicabilityRepository applicabilityRepository,
            SemesterRepository semesterRepository,
            GradeLevelRepository gradeLevelRepository,
            SchoolClassRepository schoolClassRepository) {
        this.subjectRepository = subjectRepository;
        this.applicabilityRepository = applicabilityRepository;
        this.semesterRepository = semesterRepository;
        this.gradeLevelRepository = gradeLevelRepository;
        this.schoolClassRepository = schoolClassRepository;
    }

    @Transactional
    public ResSubjectApplicabilityDTO createApplicability(Long subjectId, ReqCreateSubjectApplicabilityDTO request) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy môn học"));
        Semester semester = semesterRepository.findById(request.semesterId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy học kỳ"));
        validateTarget(subject, semester, request);
        SubjectApplicability applicability = new SubjectApplicability(
                subjectId,
                request.semesterId(),
                request.scopeType(),
                request.gradeLevelId(),
                request.classId(),
                SubjectApplicabilityStatus.ACTIVE);
        return toResponse(applicabilityRepository.save(applicability));
    }

    private void validateTarget(Subject subject, Semester semester, ReqCreateSubjectApplicabilityDTO request) {
        if (subject.getApplicationScope() != request.scopeType()) {
            throw conflict("Scope áp dụng không khớp cấu hình môn học");
        }
        if (semester.getStatus() == SemesterStatus.CLOSED) {
            throw conflict("Không cấu hình môn cho học kỳ đã CLOSED");
        }
        if (request.scopeType() == ApplicationScope.GRADE) {
            requireGradeTarget(subject.getId(), request);
        } else {
            requireClassTarget(subject, semester, request);
        }
    }

    private void requireGradeTarget(Long subjectId, ReqCreateSubjectApplicabilityDTO request) {
        if (request.gradeLevelId() == null || request.classId() != null) {
            throw conflict("Scope GRADE phải có gradeLevelId và không có classId");
        }
        if (!gradeLevelRepository.existsById(request.gradeLevelId())) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khối");
        }
        if (applicabilityRepository.existsBySubjectIdAndSemesterIdAndScopeTypeAndGradeLevelIdAndStatus(
                subjectId,
                request.semesterId(),
                ApplicationScope.GRADE,
                request.gradeLevelId(),
                SubjectApplicabilityStatus.ACTIVE)) {
            throw conflict("Cấu hình môn theo khối đã tồn tại");
        }
    }

    private void requireClassTarget(
            Subject subject,
            Semester semester,
            ReqCreateSubjectApplicabilityDTO request) {
        if (request.classId() == null || request.gradeLevelId() != null) {
            throw conflict("Scope CLASS phải có classId và không có gradeLevelId");
        }
        schoolClassRepository.findById(request.classId())
                .filter(schoolClass -> schoolClass.getAcademicYearId().equals(semester.getAcademicYearId()))
                .orElseThrow(() -> conflict("Lớp không thuộc năm học của học kỳ"));
        if (subject.getSubjectType() == SubjectType.SKILL
                && applicabilityRepository.countBySubjectIdAndStatus(
                        subject.getId(), SubjectApplicabilityStatus.ACTIVE) > 0) {
            throw conflict("Môn SKILL chỉ được cấu hình trong một học kỳ");
        }
        if (applicabilityRepository.existsBySubjectIdAndSemesterIdAndScopeTypeAndClassIdAndStatus(
                subject.getId(),
                request.semesterId(),
                ApplicationScope.CLASS,
                request.classId(),
                SubjectApplicabilityStatus.ACTIVE)) {
            throw conflict("Cấu hình môn theo lớp đã tồn tại");
        }
    }

    private ResSubjectApplicabilityDTO toResponse(SubjectApplicability applicability) {
        return new ResSubjectApplicabilityDTO(
                applicability.getId(),
                applicability.getSubjectId(),
                applicability.getSemesterId(),
                applicability.getScopeType(),
                applicability.getGradeLevelId(),
                applicability.getClassId(),
                applicability.getStatus());
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}
