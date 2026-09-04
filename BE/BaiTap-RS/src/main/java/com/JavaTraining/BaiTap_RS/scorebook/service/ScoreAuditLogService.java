package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.common.audit.domain.entity.AuditLog;
import com.JavaTraining.BaiTap_RS.common.audit.repository.AuditLogRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqFilterScoreAuditLogDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScoreAuditLogDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.repository.AssessmentColumnRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScorebookRepository;
import com.JavaTraining.BaiTap_RS.student.service.StudentLookupService;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings({
        "PMD.CouplingBetweenObjects",
        "PMD.TooManyMethods",
        "PMD.ExcessiveImports",
        "PMD.GuardLogStatement"
})
public class ScoreAuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final StudentLookupService studentLookupService;
    private final TranscriptAccessGuard accessGuard;
    private final SemesterRepository semesterRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final ScorebookRepository scorebookRepository;
    private final AssessmentColumnRepository assessmentColumnRepository;

    public ScoreAuditLogService(
            AuditLogRepository auditLogRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            StudentLookupService studentLookupService,
            TranscriptAccessGuard accessGuard,
            SemesterRepository semesterRepository,
            ClassSubjectRepository classSubjectRepository,
            ScorebookRepository scorebookRepository,
            AssessmentColumnRepository assessmentColumnRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.studentLookupService = studentLookupService;
        this.accessGuard = accessGuard;
        this.semesterRepository = semesterRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.scorebookRepository = scorebookRepository;
        this.assessmentColumnRepository = assessmentColumnRepository;
    }

    @Transactional(readOnly = true)
    public Page<ResScoreAuditLogDTO> findLogs(ReqFilterScoreAuditLogDTO filter) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScoreAuditLogService.class,
                "ScoreAuditLogService.findLogs");
        validateDateRange(filter);
        Long resolvedStudentId = resolveStudentId(filter);
        Pageable pageable = PageRequest.of(
                filter.getPage(),
                filter.getSize(),
                Sort.by(Sort.Direction.DESC, "occurredAt")
                        .and(Sort.by(Sort.Direction.DESC, "id")));
        List<AuditLog> candidates = auditLogRepository.findAll(
                AuditLogSpecifications.from(filter, resolvedStudentId), pageable.getSort());
        List<AuditLog> filtered = candidates.stream()
                .filter(log -> resolvedStudentId == null || containsStudent(log, resolvedStudentId))
                .filter(log -> !isTeacher() || isWithinTeacherScope(log))
                .toList();
        return toPage(filtered, pageable);
    }

    private Long resolveStudentId(ReqFilterScoreAuditLogDTO filter) {
        if (filter.getStudentCode() == null || filter.getStudentCode().isBlank()) {
            return filter.getStudentId();
        }
        return studentLookupService.resolveStudent(filter.getStudentId(), filter.getStudentCode()).getId();
    }

    private void validateDateRange(ReqFilterScoreAuditLogDTO filter) {
        if (filter.getFromOccurredAt() != null && filter.getToOccurredAt() != null
                && filter.getFromOccurredAt().isAfter(filter.getToOccurredAt())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Khoảng thời gian audit log không hợp lệ");
        }
    }

    private boolean containsStudent(AuditLog log, Long studentId) {
        return containsStudent(parse(log.getBeforeData()), studentId)
                || containsStudent(parse(log.getAfterData()), studentId);
    }

    private boolean containsStudent(JsonNode data, Long studentId) {
        JsonNode value = data == null ? null : data.get("studentId");
        return value != null && value.isNumber() && studentId.equals(value.longValue());
    }

    private boolean isWithinTeacherScope(AuditLog log) {
        JsonNode data = firstDataWithContext(log);
        Long studentId = longValue(data, "studentId");
        Long academicYearId = longValue(data, "academicYearId");
        if (studentId != null && academicYearId != null) {
            List<Semester> semesters = semesterRepository.findAllByAcademicYearIdOrderByDisplayOrderAsc(academicYearId);
            try {
                accessGuard.assertCanRead(studentId, academicYearId, semesters, List.of());
                return true;
            } catch (AccessDeniedException exception) {
                return false;
            }
        }

        ClassSubject classSubject = resolveClassSubject(data);
        if (classSubject == null) {
            return false;
        }
        Semester semester = semesterRepository.findById(classSubject.getSemesterId()).orElse(null);
        if (semester == null) {
            return false;
        }
        try {
            accessGuard.assertCanRead(0L, semester.getAcademicYearId(), List.of(semester), List.of(classSubject));
            return true;
        } catch (AccessDeniedException exception) {
            return false;
        }
    }

    private ClassSubject resolveClassSubject(JsonNode data) {
        Long classSubjectId = longValue(data, "classSubjectId");
        if (classSubjectId == null) {
            Long scorebookId = longValue(data, "scorebookId");
            if (scorebookId != null) {
                classSubjectId = scorebookRepository.findById(scorebookId)
                        .map(Scorebook::getClassSubjectId)
                        .orElse(null);
            }
        }
        if (classSubjectId == null) {
            Long columnId = longValue(data, "assessmentColumnId");
            if (columnId != null) {
                classSubjectId = assessmentColumnRepository.findById(columnId)
                        .flatMap(column -> scorebookRepository.findById(column.getScorebookId()))
                        .map(Scorebook::getClassSubjectId)
                        .orElse(null);
            }
        }
        return classSubjectId == null ? null : classSubjectRepository.findById(classSubjectId).orElse(null);
    }

    private JsonNode firstDataWithContext(AuditLog log) {
        JsonNode after = parse(log.getAfterData());
        return after != null && hasContext(after) ? after : parse(log.getBeforeData());
    }

    private boolean hasContext(JsonNode data) {
        return data.has("studentId") || data.has("classSubjectId")
                || data.has("scorebookId") || data.has("assessmentColumnId");
    }

    private Long longValue(JsonNode data, String field) {
        JsonNode value = data == null ? null : data.get(field);
        return value != null && value.isNumber() ? value.longValue() : null;
    }

    private boolean isTeacher() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_TEACHER".equals(authority.getAuthority()));
    }

    private Page<ResScoreAuditLogDTO> toPage(List<AuditLog> logs, Pageable pageable) {
        int from = Math.min((int) pageable.getOffset(), logs.size());
        int to = Math.min(from + pageable.getPageSize(), logs.size());
        List<AuditLog> page = logs.subList(from, to);
        Map<Long, String> actorNames = actorNames(page);
        List<ResScoreAuditLogDTO> response = page.stream()
                .map(log -> toResponse(log, actorNames))
                .toList();
        return new PageImpl<>(response, pageable, logs.size());
    }

    private Map<Long, String> actorNames(List<AuditLog> logs) {
        List<Long> actorIds = logs.stream()
                .map(AuditLog::getActorUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        return userRepository.findAllById(actorIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (left, right) -> left, HashMap::new));
    }

    private ResScoreAuditLogDTO toResponse(AuditLog log, Map<Long, String> actorNames) {
        return new ResScoreAuditLogDTO(
                log.getId(),
                log.getActorUserId(),
                actorNames.get(log.getActorUserId()),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                parse(log.getBeforeData()),
                parse(log.getAfterData()),
                log.getRequestId(),
                log.getIpAddress(),
                log.getOccurredAt());
    }

    private JsonNode parse(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Dữ liệu audit log không hợp lệ", exception);
        }
    }
}
