package com.JavaTraining.BaiTap_RS.assignment.service;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.assignment.domain.DTOs.requests.ReqCreateSubjectTeachingAssignmentDTO;
import com.JavaTraining.BaiTap_RS.assignment.domain.DTOs.requests.ReqEndAssignmentDTO;
import com.JavaTraining.BaiTap_RS.assignment.domain.DTOs.requests.ReqReplaceAssignmentDTO;
import com.JavaTraining.BaiTap_RS.assignment.domain.DTOs.response.ResSubjectTeachingAssignmentDTO;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class SubjectTeachingAssignmentService {

    private static final LocalDate OPEN_ENDED = LocalDate.of(9999, 12, 31);

    private final SubjectTeachingAssignmentRepository subjectTeachingRepository;
    private final SubjectTeachingAssignmentGuard guard;
    private final AssignmentAuditService auditService;

    public SubjectTeachingAssignmentService(
            SubjectTeachingAssignmentRepository subjectTeachingRepository,
            SubjectTeachingAssignmentGuard guard,
            AssignmentAuditService auditService) {
        this.subjectTeachingRepository = subjectTeachingRepository;
        this.guard = guard;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ResSubjectTeachingAssignmentDTO> listSubjectTeachingByTeacher(Long teacherId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SubjectTeachingAssignmentService.class,
                "SubjectTeachingAssignmentService.listSubjectTeachingByTeacher");
        return subjectTeachingRepository.findAllByTeacherIdOrderByValidFromDesc(teacherId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ResSubjectTeachingAssignmentDTO createSubjectTeachingAssignment(
            Long classSubjectId,
            ReqCreateSubjectTeachingAssignmentDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        SubjectTeachingAssignmentService.class,
                        "SubjectTeachingAssignmentService.createSubjectTeachingAssignment");
        ClassSubject classSubject = guard.findClassSubject(classSubjectId);
        Teacher teacher = guard.findActiveTeacher(request.teacherId());
        guard.validateWindowInSemester(classSubject, request.validFrom(), request.validTo());
        if (subjectTeachingRepository.existsByClassSubjectIdAndStatus(
                classSubjectId,
                AssignmentStatus.ACTIVE)) {
            throw guard.conflict("Lớp-môn đã có GVBM ACTIVE");
        }
        if (subjectTeachingRepository.existsOverlap(
                classSubjectId,
                -1L,
                request.validFrom(),
                endDate(request.validTo()))) {
            throw guard.conflict("Phân công GVBM bị chồng thời gian");
        }
        SubjectTeachingAssignment assignment = subjectTeachingRepository.save(new SubjectTeachingAssignment(
                classSubjectId,
                teacher.getId(),
                request.validFrom(),
                request.validTo(),
                AssignmentStatus.ACTIVE,
                AuditContext.currentUserId()));
        auditService.writeSubjectTeachingAudit("SUBJECT_TEACHING_ASSIGNMENT_CREATED", null, assignment);
        return toResponse(assignment);
    }

    @Transactional
    public ResSubjectTeachingAssignmentDTO replaceSubjectTeachingAssignment(
            Long assignmentId,
            ReqReplaceAssignmentDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        SubjectTeachingAssignmentService.class,
                        "SubjectTeachingAssignmentService.replaceSubjectTeachingAssignment");
        SubjectTeachingAssignment current = guard.findSubjectTeachingAssignment(assignmentId);
        if (current.getStatus() != AssignmentStatus.ACTIVE) {
            throw guard.conflict("Chỉ phân công ACTIVE mới được thay thế");
        }
        ClassSubject classSubject = guard.lockClassSubject(current.getClassSubjectId());
        Teacher teacher = guard.findActiveTeacher(request.teacherId());
        guard.validateWindowInSemester(classSubject, request.validFrom(), request.validTo());
        if (subjectTeachingRepository.existsOverlap(
                classSubject.getId(), current.getId(), request.validFrom(), endDate(request.validTo()))) {
            throw guard.conflict("Phân công GVBM bị chồng thời gian");
        }
        current.setStatus(AssignmentStatus.ENDED);
        current.setValidTo(request.validFrom().minusDays(1));
        SubjectTeachingAssignment replacement = subjectTeachingRepository.save(new SubjectTeachingAssignment(
                classSubject.getId(),
                teacher.getId(),
                request.validFrom(),
                request.validTo(),
                AssignmentStatus.ACTIVE,
                AuditContext.currentUserId()));
        auditService.writeSubjectTeachingAudit("SUBJECT_TEACHING_ASSIGNMENT_REPLACED", current, replacement);
        return toResponse(replacement);
    }

    @Transactional
    public ResSubjectTeachingAssignmentDTO endSubjectTeachingAssignment(
            Long assignmentId,
            ReqEndAssignmentDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        SubjectTeachingAssignmentService.class,
                        "SubjectTeachingAssignmentService.endSubjectTeachingAssignment");
        SubjectTeachingAssignment assignment = guard.findSubjectTeachingAssignment(assignmentId);
        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw guard.conflict("Chỉ phân công ACTIVE mới được kết thúc");
        }
        if (request.validTo().isBefore(assignment.getValidFrom())) {
            throw guard.conflict("Ngày kết thúc không được trước ngày bắt đầu");
        }
        assignment.setStatus(AssignmentStatus.ENDED);
        assignment.setValidTo(request.validTo());
        auditService.writeSubjectTeachingAudit("SUBJECT_TEACHING_ASSIGNMENT_ENDED", assignment, assignment);
        return toResponse(assignment);
    }

    private LocalDate endDate(LocalDate validTo) {
        return validTo == null ? OPEN_ENDED : validTo;
    }

    private ResSubjectTeachingAssignmentDTO toResponse(SubjectTeachingAssignment assignment) {
        return new ResSubjectTeachingAssignmentDTO(
                assignment.getId(),
                assignment.getClassSubjectId(),
                assignment.getTeacherId(),
                assignment.getValidFrom(),
                assignment.getValidTo(),
                assignment.getStatus(),
                assignment.getAssignedBy());
    }

}
