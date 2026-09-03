package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqCreateScorebookDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScorebookDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumnStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.SkillWeightConfig;
import com.JavaTraining.BaiTap_RS.scorebook.repository.AssessmentColumnRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.ScorebookRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.SkillWeightConfigRepository;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class ScorebookLifecycleService {

    private final ScorebookContext context;
    private final ScorebookRepository scorebookRepository;
    private final AssessmentColumnRepository columnRepository;
    private final SkillWeightConfigRepository weightRepository;
    private final ScorebookGuard guard;
    private final ScorebookAuditService auditService;
    private final ScorebookAuditDataMapper auditDataMapper;
    private final ScorebookResponseService responseService;
    private final ScorebookConfigurationValidator validator;

    public ScorebookLifecycleService(
            ScorebookContext context,
            ScorebookRepository scorebookRepository,
            AssessmentColumnRepository columnRepository,
            SkillWeightConfigRepository weightRepository,
            ScorebookGuard guard,
            ScorebookAuditService auditService,
            ScorebookAuditDataMapper auditDataMapper,
            ScorebookResponseService responseService,
            ScorebookConfigurationValidator validator) {
        this.context = context;
        this.scorebookRepository = scorebookRepository;
        this.columnRepository = columnRepository;
        this.weightRepository = weightRepository;
        this.guard = guard;
        this.auditService = auditService;
        this.auditDataMapper = auditDataMapper;
        this.responseService = responseService;
        this.validator = validator;
    }

    public ResScorebookDTO createScorebook(ReqCreateScorebookDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScorebookLifecycleService.class,
                "ScorebookLifecycleService.createScorebook");
        ClassSubject classSubject = context.findClassSubject(request.classSubjectId());
        context.validateClassSubject(classSubject);
        context.findActiveSubject(classSubject.getSubjectId());
        context.validateSemesterForConfiguration(classSubject.getSemesterId());
        if (scorebookRepository.existsByClassSubjectId(request.classSubjectId())) {
            throw conflict("Lớp-môn đã có sổ điểm");
        }
        Scorebook scorebook = scorebookRepository.save(new Scorebook(
                request.classSubjectId(),
                ScorebookStatus.DRAFT));
        auditService.writeAudit(
                "SCOREBOOK_CREATED",
                "scorebook",
                scorebook.getId(),
                null,
                auditDataMapper.scorebookData(scorebook));
        return responseService.toResponse(scorebook);
    }

    public ResScorebookDTO getScorebook(Long scorebookId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScorebookLifecycleService.class,
                "ScorebookLifecycleService.getScorebook");
        Scorebook scorebook = context.findScorebook(scorebookId);
        guard.assertCanRead(scorebook);
        return responseService.toResponse(scorebook);
    }

    public ResScorebookDTO getScorebookByClassSubject(Long classSubjectId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScorebookLifecycleService.class,
                "ScorebookLifecycleService.getScorebookByClassSubject");
        ClassSubject classSubject = context.findClassSubject(classSubjectId);
        guard.assertCanReadClassSubject(classSubject.getId());
        Scorebook scorebook = context.findScorebookByClassSubject(classSubjectId);
        return responseService.toResponse(scorebook);
    }

    public ResScorebookDTO openScorebook(Long scorebookId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScorebookLifecycleService.class,
                "ScorebookLifecycleService.openScorebook");
        Scorebook scorebook = context.findScorebook(scorebookId);
        guard.assertCanManage(scorebook);
        validator.ensureOpenable(scorebook);
        Map<String, Object> before = auditDataMapper.scorebookData(scorebook);
        scorebook.open();
        auditService.writeAudit(
                "SCOREBOOK_OPENED",
                "scorebook",
                scorebook.getId(),
                before,
                auditDataMapper.scorebookData(scorebook));
        return responseService.toResponse(scorebook);
    }

    public ResScorebookDTO publishScorebook(Long scorebookId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScorebookLifecycleService.class,
                "ScorebookLifecycleService.publishScorebook");
        Scorebook scorebook = context.findScorebook(scorebookId);
        guard.assertCanManage(scorebook);
        validator.ensureOpen(scorebook);
        Subject subject = context.subjectFor(scorebook);
        List<AssessmentColumn> activeColumns = columnRepository
                .findAllByScorebookIdOrderByAssessmentTypeAscColumnNoAsc(scorebookId).stream()
                .filter(column -> column.getStatus() == AssessmentColumnStatus.ACTIVE)
                .toList();
        validator.validatePublishColumns(subject, activeColumns);
        lockSkillWeightsIfNeeded(subject, scorebookId);
        publish(scorebook);
        return responseService.toResponse(scorebook);
    }

    private void lockSkillWeightsIfNeeded(Subject subject, Long scorebookId) {
        if (subject.getSubjectType() != com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType.SKILL) {
            return;
        }
        SkillWeightConfig config = weightRepository.findByScorebookId(scorebookId).orElse(null);
        if (config == null) {
            throw conflict("Môn kỹ năng chưa có cấu hình trọng số");
        }
        validator.validateWeights(
                config.getKtttWeightPercent(),
                config.getKtdkWeightPercent(),
                config.getKtckWeightPercent());
        config.lock(AuditContext.currentUserId(), LocalDateTime.now());
    }

    private void publish(Scorebook scorebook) {
        Map<String, Object> before = auditDataMapper.scorebookData(scorebook);
        scorebook.publish(AuditContext.currentUserId(), LocalDateTime.now());
        auditService.writeAudit(
                "SCOREBOOK_PUBLISHED",
                "scorebook",
                scorebook.getId(),
                before,
                auditDataMapper.scorebookData(scorebook));
    }

    private AppException conflict(String message) {
        return new AppException(org.springframework.http.HttpStatus.CONFLICT, message);
    }
}
