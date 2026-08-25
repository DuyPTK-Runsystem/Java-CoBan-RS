package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqCreateScoreChangeRequestDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqFilterScoreChangeRequestDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqRejectScoreChangeRequestDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScoreChangeRequestDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScoreChangeRequestDetailDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreChangeRequest;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreSnapshotStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScoreChangeRequestRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.StudentScoreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings({ "PMD.CouplingBetweenObjects", "PMD.TooManyMethods", "PMD.ExcessiveImports" })
public class ScoreChangeRequestService {

    private static final String ENTITY_TYPE = "ScoreChangeRequest";
    private static final String CREATE_ACTION = "CREATE_SCORE_CHANGE_REQUEST";
    private static final String APPROVE_ACTION = "APPROVE_AND_APPLY_SCORE_CHANGE_REQUEST";
    private static final String REJECT_ACTION = "REJECT_SCORE_CHANGE_REQUEST";
    private static final String CANCEL_ACTION = "CANCEL_SCORE_CHANGE_REQUEST";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_ACADEMIC_OFFICE = "ROLE_ACADEMIC_OFFICE";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ScoreChangeRequestRepository requestRepository;
    private final StudentScoreRepository scoreRepository;
    private final ScoreChangeRequestContext context;
    private final ScoreChangeRequestValidator validator;
    private final ScoreChangeRequestMapper mapper;
    private final ScorebookGuard scorebookGuard;
    private final TranscriptStateService transcriptService;
    private final CalculationTaskService taskService;
    private final ScorebookAuditService auditService;

    public ScoreChangeRequestService(
            ScoreChangeRequestRepository requestRepository,
            StudentScoreRepository scoreRepository,
            ScoreChangeRequestContext context,
            ScoreChangeRequestValidator validator,
            ScoreChangeRequestMapper mapper,
            ScorebookGuard scorebookGuard,
            TranscriptStateService transcriptService,
            CalculationTaskService taskService,
            ScorebookAuditService auditService) {
        this.requestRepository = requestRepository;
        this.scoreRepository = scoreRepository;
        this.context = context;
        this.validator = validator;
        this.mapper = mapper;
        this.scorebookGuard = scorebookGuard;
        this.transcriptService = transcriptService;
        this.taskService = taskService;
        this.auditService = auditService;
    }

    @Transactional
    public ResScoreChangeRequestDetailDTO createRequest(ReqCreateScoreChangeRequestDTO input) {
        Long actorId = currentActor();
        AssessmentColumn column = context.findActiveColumn(input.assessmentColumnId());
        Scorebook scorebook = context.findScorebook(column.getScorebookId());
        context.validateRequestableScorebook(scorebook);
        scorebookGuard.assertCanManage(scorebook);

        ClassSubject classSubject = context.findActiveClassSubject(scorebook.getClassSubjectId());
        Semester semester = context.findSemester(classSubject.getSemesterId());
        context.validateStudentAndEnrollment(input.studentId(), semester, classSubject.getClassId());
        validator.validateProposedScore(input.proposedStatus(), input.proposedValue());
        validator.validatePendingConflict(input.assessmentColumnId(), input.studentId());

        Optional<StudentScore> current = context.findScore(input.assessmentColumnId(), input.studentId());
        validator.validateDifferentFromCurrent(current, input.proposedStatus(), input.proposedValue());

        ScoreChangeRequest request = new ScoreChangeRequest(
                input.assessmentColumnId(),
                input.studentId(),
                current.map(StudentScore::getId).orElse(null),
                current.map(score -> snapshotStatus(score.getScoreStatus())).orElse(ScoreSnapshotStatus.UNSCORED),
                current.map(StudentScore::getScoreValue).orElse(null),
                input.proposedStatus(),
                input.proposedValue(),
                input.reason().trim(),
                actorId,
                now());
        ScoreChangeRequest saved = requestRepository.save(request);
        auditService.writeAudit(CREATE_ACTION, ENTITY_TYPE, saved.getId(), null, mapper.toAuditData(saved));
        return mapper.toDetail(saved);
    }

    @Transactional(readOnly = true)
    public Page<ResScoreChangeRequestDTO> findRequests(ReqFilterScoreChangeRequestDTO filter) {
        if (!isOfficeRole()) {
            filter.setRequestedBy(currentActor());
        }
        Pageable pageable = pageRequest(filter);
        return requestRepository.findAll(ScoreChangeRequestSpecifications.from(filter), pageable)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ResScoreChangeRequestDetailDTO getRequest(Long requestId) {
        ScoreChangeRequest request = findRequest(requestId);
        assertCanRead(request);
        return mapper.toDetail(request);
    }

    @Transactional
    public ResScoreChangeRequestDetailDTO approveAndApply(Long requestId) {
        Long reviewerId = currentActor();
        assertOfficeRole();
        ScoreChangeRequest request = findRequestForUpdate(requestId);
        validator.validatePending(request);
        validator.validateNotSelfReview(request, reviewerId);

        AssessmentColumn column = context.findColumn(request.getAssessmentColumnId());
        Scorebook scorebook = context.findScorebook(column.getScorebookId());
        ClassSubject classSubject = context.findActiveClassSubject(scorebook.getClassSubjectId());
        Semester semester = context.findSemester(classSubject.getSemesterId());
        context.validateStudentAndEnrollment(request.getStudentId(), semester, classSubject.getClassId());
        Optional<StudentScore> current = context.findScore(
                request.getAssessmentColumnId(), request.getStudentId());
        validator.validateSnapshotMatch(request, current);

        Map<String, Object> before = mapper.toAuditData(request);
        applyScore(request, current, reviewerId);
        request.apply(reviewerId, now());
        ScoreChangeRequest saved = requestRepository.save(request);

        long sourceVersion = transcriptService.touchTranscripts(
                request.getStudentId(), semester.getAcademicYearId(), semester.getId());
        taskService.ensureRecalcTask(request.getStudentId(), semester.getAcademicYearId(), sourceVersion);
        auditService.writeAudit(APPROVE_ACTION, ENTITY_TYPE, saved.getId(), before, mapper.toAuditData(saved));
        return mapper.toDetail(saved);
    }

    @Transactional
    public ResScoreChangeRequestDetailDTO rejectRequest(
            Long requestId, ReqRejectScoreChangeRequestDTO input) {
        Long reviewerId = currentActor();
        assertOfficeRole();
        ScoreChangeRequest request = findRequestForUpdate(requestId);
        validator.validatePending(request);
        validator.validateNotSelfReview(request, reviewerId);

        Map<String, Object> before = mapper.toAuditData(request);
        request.reject(reviewerId, now(), input.rejectionReason().trim());
        ScoreChangeRequest saved = requestRepository.save(request);
        auditService.writeAudit(REJECT_ACTION, ENTITY_TYPE, saved.getId(), before, mapper.toAuditData(saved));
        return mapper.toDetail(saved);
    }

    @Transactional
    public ResScoreChangeRequestDetailDTO cancelRequest(Long requestId) {
        Long actorId = currentActor();
        ScoreChangeRequest request = findRequestForUpdate(requestId);
        validator.validatePending(request);
        if (!actorId.equals(request.getRequestedBy()) && !hasRole(ROLE_ADMIN)) {
            throw new AppException(HttpStatus.FORBIDDEN, "Chỉ người tạo hoặc Admin được hủy yêu cầu");
        }
        Map<String, Object> before = mapper.toAuditData(request);
        request.cancel();
        ScoreChangeRequest saved = requestRepository.save(request);
        auditService.writeAudit(CANCEL_ACTION, ENTITY_TYPE, saved.getId(), before, mapper.toAuditData(saved));
        return mapper.toDetail(saved);
    }

    private StudentScore applyScore(
            ScoreChangeRequest request, Optional<StudentScore> current, Long reviewerId) {
        if (current.isPresent()) {
            StudentScore score = current.get();
            score.updateScore(request.getProposedStatus(), request.getProposedValue(), null, reviewerId);
            return scoreRepository.save(score);
        }
        StudentScore score = new StudentScore(
                request.getAssessmentColumnId(),
                request.getStudentId(),
                request.getProposedStatus(),
                request.getProposedValue(),
                null,
                reviewerId);
        return scoreRepository.save(score);
    }

    private void assertCanRead(ScoreChangeRequest request) {
        if (isOfficeRole()) {
            return;
        }
        Long actorId = currentActor();
        if (actorId.equals(request.getRequestedBy())) {
            return;
        }
        AssessmentColumn column = context.findColumn(request.getAssessmentColumnId());
        scorebookGuard.assertCanRead(context.findScorebook(column.getScorebookId()));
    }

    private ScoreChangeRequest findRequest(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu sửa điểm"));
    }

    private ScoreChangeRequest findRequestForUpdate(Long requestId) {
        return requestRepository.findForUpdate(requestId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu sửa điểm"));
    }

    private Pageable pageRequest(ReqFilterScoreChangeRequestDTO filter) {
        int page = filter.getPage() < DEFAULT_PAGE ? DEFAULT_PAGE : filter.getPage();
        int size = filter.getSize() <= 0 ? DEFAULT_PAGE_SIZE : filter.getSize();
        if (size > MAX_PAGE_SIZE) {
            size = MAX_PAGE_SIZE;
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt"));
    }

    private Long currentActor() {
        Long actorId = AuditContext.currentUserId();
        if (actorId == null) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Yêu cầu đăng nhập để thao tác yêu cầu sửa điểm");
        }
        return actorId;
    }

    private void assertOfficeRole() {
        if (!isOfficeRole()) {
            throw new AppException(HttpStatus.FORBIDDEN, "Chỉ Admin hoặc Giáo vụ được duyệt yêu cầu sửa điểm");
        }
    }

    private boolean isOfficeRole() {
        return hasRole(ROLE_ADMIN) || hasRole(ROLE_ACADEMIC_OFFICE);
    }

    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }

    private ScoreSnapshotStatus snapshotStatus(ScoreStatus status) {
        return ScoreSnapshotStatus.valueOf(status.name());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(BUSINESS_ZONE);
    }
}
