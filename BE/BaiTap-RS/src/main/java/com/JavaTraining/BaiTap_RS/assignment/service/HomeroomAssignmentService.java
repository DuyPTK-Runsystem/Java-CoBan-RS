package com.JavaTraining.BaiTap_RS.assignment.service;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.assignment.domain.DTOs.requests.ReqCreateHomeroomAssignmentDTO;
import com.JavaTraining.BaiTap_RS.assignment.domain.DTOs.requests.ReqEndAssignmentDTO;
import com.JavaTraining.BaiTap_RS.assignment.domain.DTOs.requests.ReqReplaceAssignmentDTO;
import com.JavaTraining.BaiTap_RS.assignment.domain.DTOs.response.ResHomeroomAssignmentDTO;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.HomeroomAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class HomeroomAssignmentService {

    private static final LocalDate OPEN_ENDED = LocalDate.of(9999, 12, 31);

    private final HomeroomAssignmentRepository homeroomRepository;
    private final HomeroomAssignmentGuard guard;
    private final AssignmentAuditService auditService;

    public HomeroomAssignmentService(
            HomeroomAssignmentRepository homeroomRepository,
            HomeroomAssignmentGuard guard,
            AssignmentAuditService auditService) {
        this.homeroomRepository = homeroomRepository;
        this.guard = guard;
        this.auditService = auditService;
    }

    @Transactional
    public ResHomeroomAssignmentDTO createHomeroomAssignment(
            Long classId,
            ReqCreateHomeroomAssignmentDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        HomeroomAssignmentService.class,
                        "HomeroomAssignmentService.createHomeroomAssignment");
        SchoolClass schoolClass = guard.findSchoolClass(classId);
        Teacher teacher = guard.findActiveTeacher(request.teacherId());
        guard.validateWindowInYear(schoolClass, request.validFrom(), request.validTo());
        if (homeroomRepository.existsByClassIdAndStatus(classId, AssignmentStatus.ACTIVE)) {
            throw guard.conflict("Lớp đã có GVCN ACTIVE");
        }
        if (homeroomRepository.existsOverlap(classId, -1L, request.validFrom(), endDate(request.validTo()))) {
            throw guard.conflict("Phân công GVCN bị chồng thời gian");
        }
        HomeroomAssignment assignment = homeroomRepository.save(new HomeroomAssignment(
                classId,
                teacher.getId(),
                request.validFrom(),
                request.validTo(),
                AssignmentStatus.ACTIVE,
                AuditContext.currentUserId()));
        auditService.writeHomeroomAudit("HOMEROOM_ASSIGNMENT_CREATED", null, assignment);
        return toResponse(assignment);
    }

    @Transactional
    public ResHomeroomAssignmentDTO replaceHomeroomAssignment(
            Long assignmentId,
            ReqReplaceAssignmentDTO request) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        HomeroomAssignmentService.class,
                        "HomeroomAssignmentService.replaceHomeroomAssignment");
        HomeroomAssignment current = guard.findHomeroomAssignment(assignmentId);
        if (current.getStatus() != AssignmentStatus.ACTIVE) {
            throw guard.conflict("Chỉ phân công ACTIVE mới được thay thế");
        }
        SchoolClass schoolClass = guard.lockSchoolClass(current.getClassId());
        Teacher teacher = guard.findActiveTeacher(request.teacherId());
        guard.validateWindowInYear(schoolClass, request.validFrom(), request.validTo());
        if (homeroomRepository.existsOverlap(
                schoolClass.getId(), current.getId(), request.validFrom(), endDate(request.validTo()))) {
            throw guard.conflict("Phân công GVCN bị chồng thời gian");
        }
        current.setStatus(AssignmentStatus.ENDED);
        current.setValidTo(request.validFrom().minusDays(1));
        HomeroomAssignment replacement = homeroomRepository.save(new HomeroomAssignment(
                schoolClass.getId(),
                teacher.getId(),
                request.validFrom(),
                request.validTo(),
                AssignmentStatus.ACTIVE,
                AuditContext.currentUserId()));
        auditService.writeHomeroomAudit("HOMEROOM_ASSIGNMENT_REPLACED", current, replacement);
        return toResponse(replacement);
    }

    @Transactional
    public ResHomeroomAssignmentDTO endHomeroomAssignment(Long assignmentId, ReqEndAssignmentDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                HomeroomAssignmentService.class,
                "HomeroomAssignmentService.endHomeroomAssignment");
        HomeroomAssignment assignment = guard.findHomeroomAssignment(assignmentId);
        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw guard.conflict("Chỉ phân công ACTIVE mới được kết thúc");
        }
        if (request.validTo().isBefore(assignment.getValidFrom())) {
            throw guard.conflict("Ngày kết thúc không được trước ngày bắt đầu");
        }
        assignment.setStatus(AssignmentStatus.ENDED);
        assignment.setValidTo(request.validTo());
        auditService.writeHomeroomAudit("HOMEROOM_ASSIGNMENT_ENDED", assignment, assignment);
        return toResponse(assignment);
    }

    @Transactional(readOnly = true)
    public List<ResHomeroomAssignmentDTO> listHomeroomByClass(Long classId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                HomeroomAssignmentService.class,
                "HomeroomAssignmentService.listHomeroomByClass");
        return homeroomRepository.findAllByClassIdOrderByValidFromDesc(classId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private LocalDate endDate(LocalDate validTo) {
        return validTo == null ? OPEN_ENDED : validTo;
    }

    private ResHomeroomAssignmentDTO toResponse(HomeroomAssignment assignment) {
        return new ResHomeroomAssignmentDTO(
                assignment.getId(),
                assignment.getClassId(),
                assignment.getTeacherId(),
                assignment.getValidFrom(),
                assignment.getValidTo(),
                assignment.getStatus(),
                assignment.getAssignedBy());
    }

}
