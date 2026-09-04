package com.JavaTraining.BaiTap_RS.scorebook.service;

import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqCreateAssessmentColumnDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqCreateScorebookDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqUpdateAssessmentColumnDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqUpsertSkillWeightConfigDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResAssessmentColumnDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScorebookDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class ScorebookService {

    private final ScorebookLifecycleService lifecycleService;
    private final ScorebookColumnService columnService;
    private final ScorebookSkillWeightService skillWeightService;

    public ScorebookService(
            ScorebookLifecycleService lifecycleService,
            ScorebookColumnService columnService,
            ScorebookSkillWeightService skillWeightService) {
        this.lifecycleService = lifecycleService;
        this.columnService = columnService;
        this.skillWeightService = skillWeightService;
    }

    @Transactional
    public ResScorebookDTO createScorebook(ReqCreateScorebookDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScorebookService.class,
                "ScorebookService.createScorebook");
        return lifecycleService.createScorebook(request);
    }

    @Transactional(readOnly = true)
    public ResScorebookDTO getScorebook(Long scorebookId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScorebookService.class,
                "ScorebookService.getScorebook");
        return lifecycleService.getScorebook(scorebookId);
    }

    @Transactional(readOnly = true)
    public ResScorebookDTO getScorebookByClassSubject(Long classSubjectId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScorebookService.class,
                "ScorebookService.getScorebookByClassSubject");
        return lifecycleService.getScorebookByClassSubject(classSubjectId);
    }

    @Transactional
    public ResScorebookDTO openScorebook(Long scorebookId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScorebookService.class,
                "ScorebookService.openScorebook");
        return lifecycleService.openScorebook(scorebookId);
    }

    @Transactional
    public ResAssessmentColumnDTO addColumn(Long scorebookId, ReqCreateAssessmentColumnDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScorebookService.class,
                "ScorebookService.addColumn");
        return columnService.addColumn(scorebookId, request);
    }

    @Transactional
    public ResAssessmentColumnDTO updateColumn(Long columnId, ReqUpdateAssessmentColumnDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScorebookService.class,
                "ScorebookService.updateColumn");
        return columnService.updateColumn(columnId, request);
    }

    @Transactional
    public void deactivateColumn(Long columnId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScorebookService.class,
                "ScorebookService.deactivateColumn");
        columnService.deactivateColumn(columnId);
    }

    @Transactional
    public ResScorebookDTO upsertSkillWeight(Long scorebookId, ReqUpsertSkillWeightConfigDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScorebookService.class,
                "ScorebookService.upsertSkillWeight");
        return skillWeightService.upsertSkillWeight(scorebookId, request);
    }

    @Transactional
    public ResScorebookDTO publishScorebook(Long scorebookId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScorebookService.class,
                "ScorebookService.publishScorebook");
        return lifecycleService.publishScorebook(scorebookId);
    }
}
