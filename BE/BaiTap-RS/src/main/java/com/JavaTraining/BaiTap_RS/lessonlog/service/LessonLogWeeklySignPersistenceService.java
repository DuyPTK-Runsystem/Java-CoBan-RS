package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;

import com.JavaTraining.BaiTap_RS.assignment.domain.entity.HomeroomAssignment;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.ReqWeeklyReviewDTO;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.WeeklyReviewResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogRevision;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogWeeklyReview;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.WeeklyReviewStatus;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogRevisionRepository;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogWeeklyReviewRepository;

import lombok.RequiredArgsConstructor;

/** Persists the signed weekly review and its audit record as one operation. */
@Service
@RequiredArgsConstructor
public class LessonLogWeeklySignPersistenceService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final LessonLogWeeklyReviewRepository weeks;
    private final LessonLogRevisionRepository audits;
    private final LessonLogWeeklyReviewService weeklyReviewService;
    private final LessonLogAuditService auditService;
    private final LessonLogWeeklyCommandSupport support;

    public WeeklyReviewResponse saveSigned(LessonLogWeeklyReview review, HomeroomAssignment homeroom,
            ReqWeeklyReviewDTO request, List<LessonLogEntry> entries) {
        String beforeState = weeklyReviewService.state(review);
        review.setHomeroomAssignmentId(homeroom.getId());
        review.setStatus(WeeklyReviewStatus.SIGNED);
        review.setWeeklyComment(request.weeklyComment());
        review.setWeeklyGrade(request.weeklyGrade());
        review.setSignedAt(LocalDateTime.now(ZONE));
        review.setSignedBy(AuditContext.currentUserId());
        review.setSignedSnapshotJson(weeklyReviewService.signedSnapshot(review, entries, homeroom,
                auditService::snapshot));
        LessonLogWeeklyReview savedReview = weeks.save(review);
        audits.save(new LessonLogRevision(null, savedReview.getId(), null, "SIGN_WEEK", AuditContext.currentUserId(),
                request.reason(), beforeState, weeklyReviewService.state(review)));
        return support.response(savedReview, true, null);
    }
}
