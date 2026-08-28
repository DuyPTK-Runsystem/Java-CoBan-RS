package com.JavaTraining.BaiTap_RS.academic.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateSubjectApplicabilityDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateSubjectApplicabilityDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSubjectApplicabilityDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicability;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectApplicabilityStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
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
    private final ClassSubjectRepository classSubjectRepository;
    private final AcademicCatalogAuditService auditService;
    private final SubjectApplicabilityValidator validator;

    public SubjectApplicabilityService(
            SubjectRepository subjectRepository,
            SubjectApplicabilityRepository applicabilityRepository,
            SemesterRepository semesterRepository,
            ClassSubjectRepository classSubjectRepository,
            AcademicCatalogAuditService auditService,
            SubjectApplicabilityValidator validator) {
        this.subjectRepository = subjectRepository;
        this.applicabilityRepository = applicabilityRepository;
        this.semesterRepository = semesterRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.auditService = auditService;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public List<ResSubjectApplicabilityDTO> listApplicabilities(
            Long subjectId,
            Long semesterId,
            SubjectApplicabilityStatus status) {
        findSubject(subjectId);
        List<SubjectApplicability> records = applicabilityRepository.findAllByFilters(subjectId, semesterId, status);
        return records.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ResSubjectApplicabilityDTO createApplicability(
            Long subjectId,
            ReqCreateSubjectApplicabilityDTO request) {
        Subject subject = findSubject(subjectId);
        Semester semester = findSemester(request.semesterId());
        validator.validateTarget(
                subject,
                semester,
                request.semesterId(),
                request.scopeType(),
                request.gradeLevelId(),
                request.classId(),
                null,
                SubjectApplicabilityStatus.ACTIVE);
        SubjectApplicability applicability = new SubjectApplicability(
                subjectId,
                request.semesterId(),
                request.scopeType(),
                request.gradeLevelId(),
                request.classId(),
                SubjectApplicabilityStatus.ACTIVE);
        SubjectApplicability saved = applicabilityRepository.save(applicability);
        auditService.writeAudit(
                "SUBJECT_APPLICABILITY_CREATED",
                "subject_applicability",
                saved.getId(),
                null,
                applicabilityData(saved));
        return toResponse(saved);
    }

    @Transactional
    public ResSubjectApplicabilityDTO updateApplicability(
            Long subjectId,
            Long applicabilityId,
            ReqUpdateSubjectApplicabilityDTO request) {
        Subject subject = findSubject(subjectId);
        SubjectApplicability applicability = findOwnedApplicability(subjectId, applicabilityId);
        Semester semester = findSemester(request.semesterId());
        validator.validateTarget(
                subject,
                semester,
                request.semesterId(),
                request.scopeType(),
                request.gradeLevelId(),
                request.classId(),
                applicabilityId,
                request.status());

        boolean tupleChanged = !applicability.getSemesterId().equals(request.semesterId())
                || applicability.getScopeType() != request.scopeType()
                || !Objects.equals(applicability.getGradeLevelId(), request.gradeLevelId())
                || !Objects.equals(applicability.getClassId(), request.classId());
        if (tupleChanged && classSubjectRepository.existsByApplicabilityTarget(
                subjectId,
                applicability.getSemesterId(),
                applicability.getScopeType(),
                applicability.getGradeLevelId(),
                applicability.getClassId())) {
            throw conflict("Không thể đổi học kỳ hoặc phạm vi đã được dùng cho lớp-môn");
        }

        Map<String, Object> beforeData = applicabilityData(applicability);
        applicability.setSemesterId(request.semesterId());
        applicability.setScopeType(request.scopeType());
        applicability.setGradeLevelId(request.gradeLevelId());
        applicability.setClassId(request.classId());
        applicability.setStatus(request.status());
        String action = beforeData.get("status").equals(applicability.getStatus().name())
                ? "SUBJECT_APPLICABILITY_UPDATED"
                : "SUBJECT_APPLICABILITY_STATUS_CHANGED";
        auditService.writeAudit(
                action,
                "subject_applicability",
                applicabilityId,
                beforeData,
                applicabilityData(applicability));
        return toResponse(applicability);
    }

    @Transactional
    public void deactivateApplicability(Long subjectId, Long applicabilityId) {
        findSubject(subjectId);
        SubjectApplicability applicability = findOwnedApplicability(subjectId, applicabilityId);
        Semester semester = findSemester(applicability.getSemesterId());
        validator.ensureSemesterMutable(semester);
        if (applicability.getStatus() == SubjectApplicabilityStatus.INACTIVE) {
            return;
        }
        Map<String, Object> beforeData = applicabilityData(applicability);
        applicability.setStatus(SubjectApplicabilityStatus.INACTIVE);
        auditService.writeAudit(
                "SUBJECT_APPLICABILITY_DEACTIVATED",
                "subject_applicability",
                applicabilityId,
                beforeData,
                applicabilityData(applicability));
    }

    private Subject findSubject(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy môn học"));
    }

    private Semester findSemester(Long semesterId) {
        return semesterRepository.findById(semesterId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy học kỳ"));
    }

    private SubjectApplicability findOwnedApplicability(Long subjectId, Long applicabilityId) {
        SubjectApplicability applicability = applicabilityRepository.findById(applicabilityId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy cấu hình phạm vi áp dụng"));
        if (!applicability.getSubjectId().equals(subjectId)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy cấu hình phạm vi áp dụng");
        }
        return applicability;
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

    private Map<String, Object> applicabilityData(SubjectApplicability applicability) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", applicability.getId());
        data.put("subjectId", applicability.getSubjectId());
        data.put("semesterId", applicability.getSemesterId());
        data.put("scopeType", applicability.getScopeType().name());
        data.put("gradeLevelId", applicability.getGradeLevelId());
        data.put("classId", applicability.getClassId());
        data.put("status", applicability.getStatus().name());
        return data;
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}
