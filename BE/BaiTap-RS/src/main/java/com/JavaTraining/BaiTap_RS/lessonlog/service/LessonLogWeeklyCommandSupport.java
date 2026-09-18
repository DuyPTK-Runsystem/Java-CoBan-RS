package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.ReqWeeklyReviewDTO;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogEntryResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.WeeklyReviewResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogStatus;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogWeeklyReview;

/** Keeps weekly command validation and response assembly independent of persistence orchestration. */
@Service
public class LessonLogWeeklyCommandSupport {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public void requireMonday(LocalDate date) {
        if (date == null || !date.equals(date.with(DayOfWeek.MONDAY))) {
            throw error(HttpStatus.BAD_REQUEST, "weekStart phải là thứ Hai");
        }
    }

    public void validateDateAndSemester(ReqWeeklyReviewDTO request, Semester semester) {
        if (request.weekStart().plusDays(6).isAfter(LocalDate.now(ZONE))) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "Tuần chưa kết thúc");
        }
        if (semester.getStatus() == SemesterStatus.CLOSED || semester.getStatus() == SemesterStatus.LOCKED) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "Học kỳ đã đóng");
        }
    }

    public void ensureEntriesComplete(List<LessonLogEntry> entries, List<LessonLogEntryResponse> resolvedEntries) {
        boolean hasUnlogged = resolvedEntries.stream().anyMatch(entry -> entry.status() == LessonLogStatus.UNLOGGED);
        boolean hasDraft = entries.stream().anyMatch(entry -> entry.getStatus() == LessonLogStatus.DRAFT);
        if (hasUnlogged || hasDraft) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "Tuần còn tiết chưa hoàn tất");
        }
    }

    public void assertVersion(Long actualVersion, Long expectedVersion) {
        if (!java.util.Objects.equals(actualVersion, expectedVersion)) {
            throw error(HttpStatus.CONFLICT, "Dữ liệu đã thay đổi, vui lòng tải lại");
        }
    }

    public WeeklyReviewResponse response(LessonLogWeeklyReview review, boolean canSign, String blockedReason,
            List<LessonLogEntry> entries) {
        return new WeeklyReviewResponse(review.getId(), review.getClassId(), review.getSemesterId(), review.getWeekStart(),
                review.getWeekStart().plusDays(6), review.getStatus(), review.getWeeklyComment(), review.getWeeklyGrade(),
                review.getVersion(), review.getSignedAt(), review.getSignedBy(), canSign, blockedReason,
                review.getSignedSnapshotJson(), entries.stream()
                        .sorted(java.util.Comparator.comparing(LessonLogEntry::getLessonDate)
                                .thenComparing(LessonLogEntry::getSession)
                                .thenComparing(LessonLogEntry::getPeriodIndex)
                                .thenComparing(LessonLogEntry::getId))
                        .map(entry -> new WeeklyReviewResponse.ExpectedEntry(entry.getId(), entry.getVersion()))
                        .toList());
    }

    public LocalDate homeroomScopeDate(LocalDate weekStart, Semester semester) {
        LocalDate weekEnd = weekStart.plusDays(6);
        if (semester.getEndDate() != null && weekEnd.isAfter(semester.getEndDate())) {
            return semester.getEndDate();
        }
        if (semester.getStartDate() != null && weekEnd.isBefore(semester.getStartDate())) {
            return semester.getStartDate();
        }
        return weekEnd;
    }

    public AppException error(HttpStatus status, String message) {
        return new AppException(status, message);
    }
}
