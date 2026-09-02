package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.util.Map;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqCreateAssessmentColumnDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqUpdateAssessmentColumnDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResAssessmentColumnDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumnStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.repository.AssessmentColumnRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class ScorebookColumnService {

    private final ScorebookContext context;
    private final AssessmentColumnRepository columnRepository;
    private final ScorebookGuard guard;
    private final ScorebookConfigurationValidator validator;
    private final ScorebookAuditService auditService;
    private final ScorebookAuditDataMapper auditDataMapper;
    private final ScorebookMapper mapper;

    public ScorebookColumnService(
            ScorebookContext context,
            AssessmentColumnRepository columnRepository,
            ScorebookGuard guard,
            ScorebookConfigurationValidator validator,
            ScorebookAuditService auditService,
            ScorebookAuditDataMapper auditDataMapper,
            ScorebookMapper mapper) {
        this.context = context;
        this.columnRepository = columnRepository;
        this.guard = guard;
        this.validator = validator;
        this.auditService = auditService;
        this.auditDataMapper = auditDataMapper;
        this.mapper = mapper;
    }

    public ResAssessmentColumnDTO addColumn(Long scorebookId, ReqCreateAssessmentColumnDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScorebookColumnService.class,
                "ScorebookColumnService.addColumn");
        Scorebook scorebook = context.findScorebook(scorebookId);
        guard.assertCanManage(scorebook);
        validator.ensureWritable(scorebook);
        Subject subject = context.subjectFor(scorebook);
        if (columnRepository.existsByScorebookIdAndAssessmentTypeAndColumnNo(
                scorebookId, request.assessmentType(), request.columnNo())) {
            throw conflict("Số thứ tự cột đã tồn tại trong sổ điểm");
        }
        if (subject.getSubjectType() == SubjectType.SKILL
                && columnRepository.countByScorebookIdAndAssessmentTypeAndStatus(
                        scorebookId, request.assessmentType(), AssessmentColumnStatus.ACTIVE) >= 1) {
            throw conflict("Môn kỹ năng chỉ có một cột cho mỗi loại đánh giá");
        }
        AssessmentColumn column = columnRepository.save(new AssessmentColumn(
                scorebookId,
                request.assessmentType(),
                request.columnNo(),
                request.columnName(),
                request.assessmentType().standardWeight(),
                subject.getSubjectType() == SubjectType.SKILL || request.assessmentType().isRequiredByStructure()));
        auditService.writeAudit(
                "ASSESSMENT_COLUMN_CREATED",
                "assessment_column",
                column.getId(),
                null,
                auditDataMapper.columnData(column));
        return mapper.toColumnResponse(column);
    }

    public ResAssessmentColumnDTO updateColumn(Long columnId, ReqUpdateAssessmentColumnDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScorebookColumnService.class,
                "ScorebookColumnService.updateColumn");
        AssessmentColumn column = findColumn(columnId);
        Scorebook scorebook = context.findScorebook(column.getScorebookId());
        guard.assertCanManage(scorebook);
        validator.ensureWritable(scorebook);
        if (column.getStatus() != AssessmentColumnStatus.ACTIVE) {
            throw conflict("Không thể cập nhật cột đã INACTIVE");
        }
        Map<String, Object> before = auditDataMapper.columnData(column);
        column.setColumnName(request.columnName());
        auditService.writeAudit(
                "ASSESSMENT_COLUMN_UPDATED",
                "assessment_column",
                column.getId(),
                before,
                auditDataMapper.columnData(column));
        return mapper.toColumnResponse(column);
    }

    public void deactivateColumn(Long columnId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScorebookColumnService.class,
                "ScorebookColumnService.deactivateColumn");
        AssessmentColumn column = findColumn(columnId);
        Scorebook scorebook = context.findScorebook(column.getScorebookId());
        guard.assertCanManage(scorebook);
        validator.ensureWritable(scorebook);
        if (column.getStatus() != AssessmentColumnStatus.ACTIVE) {
            throw conflict("Cột điểm đã INACTIVE");
        }
        Map<String, Object> before = auditDataMapper.columnData(column);
        column.deactivate();
        auditService.writeAudit(
                "ASSESSMENT_COLUMN_DEACTIVATED",
                "assessment_column",
                column.getId(),
                before,
                auditDataMapper.columnData(column));
    }

    private AssessmentColumn findColumn(Long columnId) {
        return columnRepository.findById(columnId)
                .orElseThrow(() -> new com.JavaTraining.BaiTap_RS.common.error.AppException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy cột điểm"));
    }

    private com.JavaTraining.BaiTap_RS.common.error.AppException conflict(String message) {
        return new com.JavaTraining.BaiTap_RS.common.error.AppException(HttpStatus.CONFLICT, message);
    }
}
