package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.HomeroomAssignment;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogWeeklyReview;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.WeeklyReviewStatus;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogRevisionRepository;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogWeeklyReviewRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;

import lombok.RequiredArgsConstructor;

/** Owns the weekly review aggregate lifecycle and signing invariants. */
@Service
@RequiredArgsConstructor
public class LessonLogWeeklyReviewService {
    private final LessonLogWeeklyReviewRepository weeklyReviews;
    private final LessonLogRevisionRepository audits;
    private final LessonLogSourceResolver sourceResolver;
    private final HomeroomAssignmentRepository homerooms;
    private final LessonLogWeeklySnapshotService snapshotService;

    public void ensureExists(LessonLogEntry entry) {
        sourceResolver.lockForEntry(entry);
        weeklyReviews.findByClassIdAndSemesterIdAndWeekStartForUpdate(entry.getClassId(), entry.getSemesterId(),
                entry.getLessonDate().with(DayOfWeek.MONDAY)).orElseGet(() -> {
                    LessonLogWeeklyReview review = new LessonLogWeeklyReview();
                    review.setClassId(entry.getClassId());
                    review.setSemesterId(entry.getSemesterId());
                    review.setWeekStart(entry.getLessonDate().with(DayOfWeek.MONDAY));
                    homerooms.findFirstActiveAt(entry.getClassId(), AssignmentStatus.ACTIVE,
                            entry.getLessonDate().with(DayOfWeek.MONDAY))
                            .ifPresent(assignment -> review.setHomeroomAssignmentId(assignment.getId()));
                    return weeklyReviews.save(review);
                });
    }

    public void invalidate(LessonLogEntry entry) {
        sourceResolver.lockForEntry(entry);
        LessonLogWeeklyReview review = weeklyReviews.findByClassIdAndSemesterIdAndWeekStartForUpdate(
                entry.getClassId(), entry.getSemesterId(), entry.getLessonDate().with(DayOfWeek.MONDAY)).orElse(null);
        if (review != null && review.getStatus() == WeeklyReviewStatus.SIGNED) {
            String before = state(review);
            review.setStatus(WeeklyReviewStatus.STALE);
            weeklyReviews.save(review);
            audits.save(new com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogRevision(null, review.getId(), null,
                    "INVALIDATE_WEEK", actor(), "Nội dung tiết đã thay đổi", before, state(review)));
        }
    }

    public LessonLogWeeklyReview loadForUpdate(Long classId, Long semesterId, LocalDate weekStart) {
        return weeklyReviews.findByClassIdAndSemesterIdAndWeekStartForUpdate(classId, semesterId, weekStart)
                .orElseGet(() -> {
                    LessonLogWeeklyReview review = new LessonLogWeeklyReview();
                    review.setClassId(classId);
                    review.setSemesterId(semesterId);
                    review.setWeekStart(weekStart);
                    return review;
                });
    }

    public void lockEntries(List<LessonLogEntry> entries, Long semesterId) {
        sourceResolver.lockForEntries(entries, semesterId);
    }

    public String state(LessonLogWeeklyReview review) {
        return snapshotService.state(review);
    }

    public String signedSnapshot(LessonLogWeeklyReview review, List<LessonLogEntry> entries,
            HomeroomAssignment homeroom, Function<LessonLogEntry, Map<String, Object>> entrySnapshot) {
        return snapshotService.signedSnapshot(review, entries, homeroom, entrySnapshot);
    }
    private Long actor() { return AuditContext.currentUserId(); }
}
