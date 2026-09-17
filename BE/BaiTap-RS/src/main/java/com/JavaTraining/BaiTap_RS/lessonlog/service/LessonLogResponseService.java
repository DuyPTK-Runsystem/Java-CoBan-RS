package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogEntryResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogRevisionResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.WeeklyReviewResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogPolicy;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogRevision;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogWeeklyReview;

import lombok.RequiredArgsConstructor;

/** Shared response facade for lesson-log read and write flows. */
@Service
@RequiredArgsConstructor
public class LessonLogResponseService {
    private final LessonLogResponseMappingService mappingService;
    private final LessonLogResponseAuditService auditService;

    public LessonLogEntryResponse map(LessonLogEntry entry) {
        return mappingService.map(entry);
    }

    public List<Object> rubric(LessonLogPolicy policy) {
        return mappingService.rubric(policy);
    }

    public WeeklyReviewResponse weekly(LessonLogWeeklyReview review, boolean canSign, String blockedReason) {
        return auditService.weekly(review, canSign, blockedReason);
    }

    public ResultPaginationDTO<LessonLogRevisionResponse> auditPage(Page<LessonLogRevision> page) {
        return auditService.auditPage(page);
    }
}
