package com.JavaTraining.BaiTap_RS.academic.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateSubjectDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateSubjectDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSubjectDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final AcademicCatalogAuditService auditService;

    public SubjectService(
            SubjectRepository subjectRepository,
            AcademicCatalogAuditService auditService) {
        this.subjectRepository = subjectRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ResSubjectDTO> listSubjects(SubjectStatus status) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SubjectService.class,
                "SubjectService.listSubjects");
        List<Subject> subjects = status == null
                ? subjectRepository.findAllByOrderByCodeAsc()
                : subjectRepository.findAllByStatusOrderByCodeAsc(status);
        return subjects.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ResSubjectDTO createSubject(ReqCreateSubjectDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SubjectService.class,
                "SubjectService.createSubject");
        if (subjectRepository.existsByCode(request.code())) {
            throw conflict("Mã môn học đã tồn tại");
        }
        Subject subject = new Subject(
                request.code(),
                request.name(),
                request.subjectType(),
                request.applicationScope(),
                request.status());
        Subject saved = subjectRepository.save(subject);
        auditService.writeAudit("SUBJECT_CREATED", "subject", saved.getId(), null, subjectData(saved));
        return toResponse(saved);
    }

    @Transactional
    public ResSubjectDTO updateSubject(Long id, ReqUpdateSubjectDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SubjectService.class,
                "SubjectService.updateSubject");
        Subject subject = findSubject(id);
        if (subjectRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw conflict("Mã môn học đã tồn tại");
        }
        Map<String, Object> beforeData = subjectData(subject);
        subject.setCode(request.code());
        subject.setName(request.name());
        subject.setSubjectType(request.subjectType());
        subject.setApplicationScope(request.applicationScope());
        subject.setStatus(request.status());
        String action = beforeData.get("status").equals(subject.getStatus().name())
                ? "SUBJECT_UPDATED"
                : "SUBJECT_STATUS_CHANGED";
        auditService.writeAudit(action, "subject", subject.getId(), beforeData, subjectData(subject));
        return toResponse(subject);
    }

    private Subject findSubject(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy môn học"));
    }

    private ResSubjectDTO toResponse(Subject subject) {
        return new ResSubjectDTO(
                subject.getId(),
                subject.getCode(),
                subject.getName(),
                subject.getSubjectType(),
                subject.getApplicationScope(),
                subject.getStatus());
    }

    private Map<String, Object> subjectData(Subject subject) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", subject.getId());
        data.put("code", subject.getCode());
        data.put("name", subject.getName());
        data.put("subjectType", subject.getSubjectType().name());
        data.put("applicationScope", subject.getApplicationScope().name());
        data.put("status", subject.getStatus().name());
        return data;
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}
