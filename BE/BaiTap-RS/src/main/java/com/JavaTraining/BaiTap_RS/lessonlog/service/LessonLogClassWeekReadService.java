package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogClassWeekResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogEntryResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogStatus;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogWeeklyReview;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogEntryRepository;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogWeeklyReviewRepository;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LessonLogClassWeekReadService {
    private static final String SEMESTER_NOT_FOUND = "Không tìm thấy học kỳ";

    private final SemesterRepository semesters;
    private final LessonLogEntryRepository entries;
    private final LessonLogWeeklyReviewRepository weeks;
    private final LessonLogScheduleService scheduleService;
    private final LessonLogWeeklySigningPolicyService weeklySigningPolicy;
    private final LessonLogResponseService responseService;
    private final TeacherRepository teachers;
    private final HomeroomAssignmentRepository homerooms;
    private final StudentYearEnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public LessonLogClassWeekResponse classWeek(Long classId, Long semesterId, LocalDate weekStart) {
        requireMonday(weekStart);
        Semester semester = semesters.findById(semesterId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, SEMESTER_NOT_FOUND));
        LessonLogScopeAuthorization.assertClassScope(classId, homeroomScopeDate(weekStart, semester), teachers, homerooms);
        List<LessonLogEntry> loggedEntries = entries
                .findByClassIdAndSemesterIdAndLessonDateBetweenOrderByLessonDateAscPeriodIndexAsc(
                        classId, semesterId, weekStart, weekStart.plusDays(6));
        LessonLogWeeklyReview review = weeks.findByClassIdAndSemesterIdAndWeekStart(classId, semesterId, weekStart)
                .orElse(null);
        List<LessonLogEntryResponse> resolvedEntries = scheduleService.classWeekItems(classId, semester, weekStart,
                weekStart.plusDays(6), loggedEntries, responseService::map, this::roster, responseService::rubric);
        boolean hasUnloggedEntries = resolvedEntries.stream()
                .anyMatch(item -> item.status() == LessonLogStatus.UNLOGGED);
        com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.WeeklyReviewResponse weeklyReview = review == null ? null : responseService.weekly(review,
                !hasUnloggedEntries && weeklySigningPolicy.canSign(review, loggedEntries, semester, weekStart),
                hasUnloggedEntries ? "Tuần còn tiết chưa ghi"
                        : weeklySigningPolicy.blockedReason(loggedEntries, semester, weekStart));
        return new LessonLogClassWeekResponse(classId, semesterId, weekStart, weekStart.plusDays(6),
                scheduleService.calendarDays(semester, weekStart), resolvedEntries, weeklyReview);
    }

    private void requireMonday(LocalDate date) {
        if (date == null || !date.equals(date.with(DayOfWeek.MONDAY))) {
            throw error(HttpStatus.BAD_REQUEST, "weekStart phải là thứ Hai");
        }
    }

    private LocalDate homeroomScopeDate(LocalDate weekStart, Semester semester) {
        LocalDate weekEnd = weekStart.plusDays(6);
        if (semester.getEndDate() != null && weekEnd.isAfter(semester.getEndDate())) {
            return semester.getEndDate();
        }
        if (semester.getStartDate() != null && weekEnd.isBefore(semester.getStartDate())) {
            return semester.getStartDate();
        }
        return weekEnd;
    }

    private int roster(Long classId, LocalDateTime at) {
        return Math.toIntExact(enrollmentRepository.countRosterAt(classId, at));
    }

    private AppException error(HttpStatus status, String message) {
        return new AppException(status, message);
    }
}
