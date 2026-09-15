package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.HomeroomAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.ReqWeeklyReviewDTO;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogStatus;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogWeeklyReview;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.WeeklyReviewStatus;

import lombok.RequiredArgsConstructor;

/** Validates weekly signing state and resolves its homeroom assignment. */
@Service
@RequiredArgsConstructor
public class LessonLogWeeklySigningPolicyService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final HomeroomAssignmentRepository homerooms;

    public void validateExpectedEntries(LessonLogWeeklyReview review, ReqWeeklyReviewDTO request,
            List<LessonLogEntry> entries) {
        if (request.expectedEntries() == null) {
            throw conflict("Tập tiết đã thay đổi, vui lòng tải lại");
        }
        Set<Long> expectedIds = request.expectedEntries().stream()
                .map(ReqWeeklyReviewDTO.ExpectedEntry::entryId).collect(Collectors.toSet());
        boolean changed = expectedIds.size() != request.expectedEntries().size() || expectedIds.size() != entries.size()
                || request.expectedEntries().stream().anyMatch(expected -> entries.stream().noneMatch(entry ->
                        Objects.equals(entry.getId(), expected.entryId())
                                && Objects.equals(entry.getVersion(), expected.version())));
        if (changed) {
            throw conflict("Tập tiết đã thay đổi, vui lòng tải lại");
        }
        if (review.getStatus() == WeeklyReviewStatus.STALE && blank(request.reason())) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "Ký lại phải có lý do");
        }
    }

    public HomeroomAssignment activeHomeroom(Long classId, LocalDate weekStart) {
        return homerooms.findFirstActiveAt(classId, AssignmentStatus.ACTIVE, weekStart)
                .orElseThrow(() -> new AppException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Chưa xác định được giáo viên chủ nhiệm tại tuần cần ký"));
    }

    public void validateHomeroomChange(LessonLogWeeklyReview review, HomeroomAssignment homeroom,
            ReqWeeklyReviewDTO request) {
        boolean changed = review.getHomeroomAssignmentId() != null
                && !Objects.equals(review.getHomeroomAssignmentId(), homeroom.getId());
        if (changed && review.getStatus() == WeeklyReviewStatus.SIGNED && blank(request.reason())) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Phân công giáo viên chủ nhiệm đã thay đổi; ký lại phải có lý do");
        }
    }

    public boolean canSign(LessonLogWeeklyReview review, List<LessonLogEntry> entries,
            Semester semester, LocalDate weekStart) {
        return review.getStatus() != WeeklyReviewStatus.SIGNED && !entries.isEmpty()
                && !weekStart.plusDays(6).isAfter(LocalDate.now(ZONE))
                && semester.getStatus() != SemesterStatus.CLOSED
                && semester.getStatus() != SemesterStatus.LOCKED
                && entries.stream().noneMatch(entry -> entry.getStatus() == LessonLogStatus.DRAFT);
    }

    public String blockedReason(List<LessonLogEntry> entries, Semester semester, LocalDate weekStart) {
        if (entries.isEmpty()) {
            return "Tuần chưa có tiết";
        }
        if (weekStart.plusDays(6).isAfter(LocalDate.now(ZONE))) {
            return "Tuần chưa kết thúc";
        }
        if (semester.getStatus() == SemesterStatus.CLOSED || semester.getStatus() == SemesterStatus.LOCKED) {
            return "Học kỳ đã đóng";
        }
        return entries.stream().anyMatch(entry -> entry.getStatus() == LessonLogStatus.DRAFT)
                ? "Tuần còn sổ nháp" : null;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}
