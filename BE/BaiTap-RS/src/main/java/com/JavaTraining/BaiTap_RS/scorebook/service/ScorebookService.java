package com.JavaTraining.BaiTap_RS.scorebook.service;

import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqCreateAssessmentColumnDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqCreateScorebookDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqUpdateAssessmentColumnDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqUpsertSkillWeightConfigDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResAssessmentColumnDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScorebookDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
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
        return lifecycleService.createScorebook(request);
    }

    @Transactional(readOnly = true)
    public ResScorebookDTO getScorebook(Long scorebookId) {
        return lifecycleService.getScorebook(scorebookId);
    }

    @Transactional
    public ResScorebookDTO openScorebook(Long scorebookId) {
        return lifecycleService.openScorebook(scorebookId);
    }

    @Transactional
    public ResAssessmentColumnDTO addColumn(Long scorebookId, ReqCreateAssessmentColumnDTO request) {
        return columnService.addColumn(scorebookId, request);
    }

    @Transactional
    public ResAssessmentColumnDTO updateColumn(Long columnId, ReqUpdateAssessmentColumnDTO request) {
        return columnService.updateColumn(columnId, request);
    }

    @Transactional
    public void deactivateColumn(Long columnId) {
        columnService.deactivateColumn(columnId);
    }

    @Transactional
    public ResScorebookDTO upsertSkillWeight(Long scorebookId, ReqUpsertSkillWeightConfigDTO request) {
        return skillWeightService.upsertSkillWeight(scorebookId, request);
    }

    @Transactional
    public ResScorebookDTO publishScorebook(Long scorebookId) {
        return lifecycleService.publishScorebook(scorebookId);
    }
}
