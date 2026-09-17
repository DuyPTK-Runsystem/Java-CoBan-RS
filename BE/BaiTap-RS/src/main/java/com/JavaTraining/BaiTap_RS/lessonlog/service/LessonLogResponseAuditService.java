package com.JavaTraining.BaiTap_RS.lessonlog.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogRevisionResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.WeeklyReviewResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogRevision;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogWeeklyReview;

@Service
public class LessonLogResponseAuditService {
    public WeeklyReviewResponse weekly(LessonLogWeeklyReview review, boolean canSign, String blockedReason) {
        return new WeeklyReviewResponse(review.getId(), review.getClassId(), review.getSemesterId(), review.getWeekStart(),
                review.getWeekStart().plusDays(6), review.getStatus(), review.getWeeklyComment(), review.getWeeklyGrade(),
                review.getVersion(), review.getSignedAt(), review.getSignedBy(), canSign, blockedReason,
                review.getSignedSnapshotJson());
    }

    public ResultPaginationDTO<LessonLogRevisionResponse> auditPage(Page<LessonLogRevision> page) {
        return new ResultPaginationDTO<>(new ResultPaginationDTO.Meta(page.getNumber(), page.getSize(), page.getTotalPages(),
                page.getTotalElements()), page.getContent().stream().map(value -> new LessonLogRevisionResponse(value.getId(),
                value.getEntryId(), value.getWeeklyReviewId(), value.getPolicyId(), value.getAction(), value.getActorId(),
                value.getReason(), value.getBeforeStateJson(), value.getAfterStateJson(), value.getCorrelationId(),
                value.getCreatedAt())).toList());
    }
}
