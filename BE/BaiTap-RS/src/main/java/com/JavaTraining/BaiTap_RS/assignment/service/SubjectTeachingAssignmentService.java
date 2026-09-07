package com.JavaTraining.BaiTap_RS.assignment.service;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
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
@SuppressWarnings({"PMD.GuardLogStatement", "PMD.TooManyMethods"})
public class SubjectTeachingAssignmentService {

    private static final LocalDate OPEN_ENDED = LocalDate.of(9999, 12, 31);

    private final SubjectTeachingAssignmentRepository subjectTeachingRepository;
    private final SubjectTeachingAssignmentGuard guard;
    private final AssignmentAuditService auditService;
    private final ClassSubjectRepository classSubjectRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;

    public SubjectTeachingAssignmentService(
            SubjectTeachingAssignmentRepository subjectTeachingRepository,
            SubjectTeachingAssignmentGuard guard,
            AssignmentAuditService auditService,
            ClassSubjectRepository classSubjectRepository,
            SchoolClassRepository schoolClassRepository,
            SubjectRepository subjectRepository) {
        this.subjectTeachingRepository = subjectTeachingRepository;
        this.guard = guard;
        this.auditService = auditService;
        this.classSubjectRepository = classSubjectRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
    }

    @Transactional(readOnly = true)
    public List<ResSubjectTeachingAssignmentDTO> listSubjectTeachingByTeacher(Long teacherId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                SubjectTeachingAssignmentService.class,
                "SubjectTeachingAssignmentService.listSubjectTeachingByTeacher");
        List<SubjectTeachingAssignment> assignments = subjectTeachingRepository
                .findAllByTeacherIdOrderByValidFromDesc(teacherId);
        return assignments.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ResSubjectTeachingAssignmentDTO> listSubjectTeachingByClassAndSemester(
            Long classId,
            Long semesterId) {
        List<SubjectTeachingAssignment> assignments = subjectTeachingRepository
                .findAllByClassIdAndSemesterIdOrderByValidFromDesc(classId, semesterId);
        return assignments.stream().map(this::toResponse).toList();
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
        ClassSubject classSubject = classSubjectRepository.findById(assignment.getClassSubjectId()).orElse(null);
        SchoolClass schoolClass = classSubject == null ? null
                : schoolClassRepository.findById(classSubject.getClassId()).orElse(null);
        Subject subject = classSubject == null
                ? null
                : subjectRepository.findById(classSubject.getSubjectId()).orElse(null);
        return new ResSubjectTeachingAssignmentDTO(
                assignment.getId(),
                assignment.getClassSubjectId(),
                assignment.getTeacherId(),
                assignment.getValidFrom(),
                assignment.getValidTo(),
                assignment.getStatus(),
                assignment.getAssignedBy(),
                classId(classSubject),
                className(schoolClass),
                classCode(schoolClass),
                subjectId(classSubject),
                subjectName(subject),
                semesterId(classSubject));
    }

    private Long classId(ClassSubject classSubject) {
        return classSubject == null ? null : classSubject.getClassId();
    }

    private String className(SchoolClass schoolClass) {
        return schoolClass == null ? null : schoolClass.getClassName();
    }

    private String classCode(SchoolClass schoolClass) {
        return schoolClass == null ? null : schoolClass.getClassCode();
    }

    private Long subjectId(ClassSubject classSubject) {
        return classSubject == null ? null : classSubject.getSubjectId();
    }

    private String subjectName(Subject subject) {
        return subject == null ? null : subject.getName();
    }

    private Long semesterId(ClassSubject classSubject) {
        return classSubject == null ? null : classSubject.getSemesterId();
    }

}
