package com.JavaTraining.BaiTap_RS.scorebook.service;

import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResScorebookDTO;
import com.JavaTraining.BaiTap_RS.scorebook.repository.AssessmentColumnRepository;
import com.JavaTraining.BaiTap_RS.scorebook.repository.SkillWeightConfigRepository;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("PMD.GuardLogStatement")
public class ScorebookResponseService {

    private final AssessmentColumnRepository columnRepository;
    private final SkillWeightConfigRepository weightRepository;
    private final ScorebookMapper mapper;

    public ScorebookResponseService(
            AssessmentColumnRepository columnRepository,
            SkillWeightConfigRepository weightRepository,
            ScorebookMapper mapper) {
        this.columnRepository = columnRepository;
        this.weightRepository = weightRepository;
        this.mapper = mapper;
    }

    public ResScorebookDTO toResponse(com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook scorebook) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                ScorebookResponseService.class,
                "ScorebookResponseService.toResponse");
        return mapper.toScorebookResponse(
                scorebook,
                columnRepository.findAllByScorebookIdOrderByAssessmentTypeAscColumnNoAsc(scorebook.getId()).stream()
                        .map(mapper::toColumnResponse)
                        .toList(),
                mapper.toWeightResponse(weightRepository.findByScorebookId(scorebook.getId()).orElse(null)));
    }
}
