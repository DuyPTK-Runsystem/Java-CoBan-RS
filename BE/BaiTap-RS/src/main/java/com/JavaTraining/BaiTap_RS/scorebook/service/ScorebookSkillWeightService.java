package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.time.LocalDateTime;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqUpsertSkillWeightConfigDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScorebookDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.SkillWeightConfig;
import com.JavaTraining.BaiTap_RS.scorebook.repository.SkillWeightConfigRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ScorebookSkillWeightService {

    private final ScorebookContext context;
    private final SkillWeightConfigRepository weightRepository;
    private final ScorebookGuard guard;
    private final ScorebookConfigurationValidator validator;
    private final ScorebookAuditService auditService;
    private final ScorebookAuditDataMapper auditDataMapper;
    private final ScorebookResponseService responseService;

    public ScorebookSkillWeightService(
            ScorebookContext context,
            SkillWeightConfigRepository weightRepository,
            ScorebookGuard guard,
            ScorebookConfigurationValidator validator,
            ScorebookAuditService auditService,
            ScorebookAuditDataMapper auditDataMapper,
            ScorebookResponseService responseService) {
        this.context = context;
        this.weightRepository = weightRepository;
        this.guard = guard;
        this.validator = validator;
        this.auditService = auditService;
        this.auditDataMapper = auditDataMapper;
        this.responseService = responseService;
    }

    public ResScorebookDTO upsertSkillWeight(Long scorebookId, ReqUpsertSkillWeightConfigDTO request) {
        Scorebook scorebook = context.findScorebook(scorebookId);
        guard.assertCanManage(scorebook);
        validator.ensureWritable(scorebook);
        if (context.subjectFor(scorebook).getSubjectType() != SubjectType.SKILL) {
            throw conflict("Chỉ môn SKILL mới được cấu hình trọng số kỹ năng");
        }
        validator.validateWeights(request);
        Long actorId = requiredActorId();
        LocalDateTime now = LocalDateTime.now();
        SkillWeightConfig config = weightRepository.findByScorebookId(scorebookId).orElse(null);
        Map<String, Object> before = config == null ? null : auditDataMapper.weightData(config);
        if (config == null) {
            config = weightRepository.save(new SkillWeightConfig(
                    scorebookId,
                    request.ktttWeightPercent(),
                    request.ktdkWeightPercent(),
                    request.ktckWeightPercent(),
                    actorId,
                    now));
        } else {
            updateExisting(config, request, actorId, now);
        }
        auditService.writeAudit(
                "SKILL_WEIGHT_CONFIGURED",
                "skill_weight_config",
                config.getId(),
                before,
                auditDataMapper.weightData(config));
        return responseService.toResponse(scorebook);
    }

    private void updateExisting(
            SkillWeightConfig config,
            ReqUpsertSkillWeightConfigDTO request,
            Long actorId,
            LocalDateTime now) {
        if (config.getLockedAt() != null) {
            throw conflict("Không thể thay đổi trọng số đã khóa");
        }
        config.update(
                request.ktttWeightPercent(),
                request.ktdkWeightPercent(),
                request.ktckWeightPercent(),
                actorId,
                now);
    }

    private Long requiredActorId() {
        Long actorId = AuditContext.currentUserId();
        if (actorId == null) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Yêu cầu đăng nhập để cấu hình trọng số");
        }
        return actorId;
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}
