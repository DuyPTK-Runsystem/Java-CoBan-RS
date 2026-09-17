package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogRevisionResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogRevisionRepository;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogWeeklyReviewRepository;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LessonLogRevisionReadService {
    private static final String SEMESTER_NOT_FOUND = "Không tìm thấy học kỳ";

    private final LessonLogRevisionRepository audits;
    private final LessonLogWeeklyReviewRepository weeks;
    private final SemesterRepository semesters;
    private final TeacherRepository teachers;
    private final HomeroomAssignmentRepository homerooms;
    private final LessonLogResponseService responseService;

    @Transactional(readOnly = true)
    public ResultPaginationDTO<LessonLogRevisionResponse> weeklyRevisions(Long classId, Long semesterId,
            LocalDate weekStart, int page, int size) {
        requireMonday(weekStart);
        Semester semester = semesters.findById(semesterId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, SEMESTER_NOT_FOUND));
        LessonLogScopeAuthorization.assertClassScope(classId, homeroomScopeDate(weekStart, semester), teachers, homerooms);
        Long reviewId = weeks.findByClassIdAndSemesterIdAndWeekStart(classId, semesterId, weekStart)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Chưa có tổng kết tuần")).getId();
        return responseService.auditPage(audits.findByWeeklyReviewIdOrderByCreatedAtDesc(reviewId, paging(page, size)));
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO<LessonLogRevisionResponse> policyRevisions(int page, int size) {
        return responseService.auditPage(audits.findPolicyRevisions(paging(page, size)));
    }

    private PageRequest paging(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "createdAt"));
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

    private AppException error(HttpStatus status, String message) {
        return new AppException(status, message);
    }
}
