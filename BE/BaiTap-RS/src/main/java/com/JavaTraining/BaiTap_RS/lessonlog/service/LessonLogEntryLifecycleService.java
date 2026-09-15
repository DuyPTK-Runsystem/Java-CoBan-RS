package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.ReqTransitionLessonLogDTO;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.ReqUpdateLessonLogDTO;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogStatus;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogEntryRepository;

import lombok.RequiredArgsConstructor;

/** Owns state-changing entry operations after the facade authorizes the request. */
@Service
@RequiredArgsConstructor
public class LessonLogEntryLifecycleService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final LessonLogEntryRepository entries;
    private final LessonLogEntryEditor editor;
    private final LessonLogAuditService auditService;
    private final LessonLogWeeklyReviewService weeklyReviews;

    public LessonLogEntry update(LessonLogEntry entry, ReqUpdateLessonLogDTO request) {
        String beforeState = auditService.state(entry);
        editor.apply(entry, new LessonLogEntryContent(request.title(), request.content(), request.completionStatus(),
                request.grade(), request.presentCount(), request.absentCount(), request.comments(),
                request.absentStudentNotes(), request.homework()));
        entries.save(entry);
        auditService.record(entry, new LessonLogAuditChange("UPDATE", null, beforeState, auditService.state(entry)));
        weeklyReviews.invalidate(entry);
        return entry;
    }

    public LessonLogEntry submit(LessonLogEntry entry) {
        editor.validateComplete(entry);
        String beforeState = auditService.state(entry);
        entry.setStatus(LessonLogStatus.SUBMITTED);
        entry.setSubmittedAt(now());
        entry.setSubmittedBy(actor());
        entries.save(entry);
        auditService.record(entry, new LessonLogAuditChange("SUBMIT", null, beforeState, auditService.state(entry)));
        weeklyReviews.invalidate(entry);
        return entry;
    }

    public LessonLogEntry review(LessonLogEntry entry, ReqTransitionLessonLogDTO request) {
        String beforeState = auditService.state(entry);
        entry.setStatus(LessonLogStatus.REVIEWED);
        entry.setReviewComment(request.comment());
        entry.setReviewedAt(now());
        entry.setReviewedBy(actor());
        entries.save(entry);
        auditService.record(entry, new LessonLogAuditChange("REVIEW", request.reason(), beforeState, auditService.state(entry)));
        weeklyReviews.invalidate(entry);
        return entry;
    }

    public LessonLogEntry amend(LessonLogEntry entry, ReqTransitionLessonLogDTO request) {
        editor.apply(entry, new LessonLogEntryContent(request.title(), request.content(), request.completionStatus(),
                request.grade(), request.presentCount(), request.absentCount(), request.comments(),
                request.absentStudentNotes(), request.homework()));
        editor.validateComplete(entry);
        String beforeState = auditService.state(entry);
        entry.setStatus(LessonLogStatus.AMENDED);
        entry.setReviewedAt(null);
        entry.setReviewedBy(null);
        entries.save(entry);
        auditService.record(entry, new LessonLogAuditChange("AMEND", request.reason(), beforeState, auditService.state(entry)));
        weeklyReviews.invalidate(entry);
        return entry;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    private Long actor() {
        return AuditContext.currentUserId();
    }
}
