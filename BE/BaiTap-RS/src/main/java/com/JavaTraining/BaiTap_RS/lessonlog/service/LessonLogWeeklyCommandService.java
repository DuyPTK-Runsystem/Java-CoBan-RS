package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;

import org.springframework.stereotype.Service;

import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.ReqWeeklyReviewDTO;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogEntryResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.WeeklyReviewResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogPolicy;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogWeeklyReview;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogEntryRepository;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;

import lombok.RequiredArgsConstructor;

/** Provides a narrow command boundary for weekly signing. */
@Service
@RequiredArgsConstructor
public class LessonLogWeeklyCommandService {
        private final LessonLogEntryRepository entries;
        private final SemesterRepository semesters;
        private final HomeroomAssignmentRepository homerooms;
        private final TeacherRepository teachers;
        private final LessonLogWeeklyReviewService weeklyReviewService;
        private final LessonLogWeeklySigningPolicyService weeklySigningPolicy;
        private final LessonLogScheduleService scheduleService;
        private final LessonLogWeeklyCommandSupport support;
        private final LessonLogWeeklySignPersistenceService signPersistence;

        public WeeklyReviewResponse signWeek(Long classId, ReqWeeklyReviewDTO request,
                        Function<LessonLogEntry, LessonLogEntryResponse> mapper,
                        ToIntBiFunction<Long, LocalDateTime> rosterCounter,
                        Function<LessonLogPolicy, List<Object>> rubricMapper) {
                support.requireMonday(request.weekStart());
                Semester semester = semesters.findById(request.semesterId())
                                .orElseThrow(() -> support.error(org.springframework.http.HttpStatus.NOT_FOUND,
                                                "Không tìm thấy học kỳ"));
                LessonLogScopeAuthorization.assertClassScope(classId,
                                support.homeroomScopeDate(request.weekStart(), semester),
                                teachers, homerooms);
                support.validateDateAndSemester(request, semester);
                List<LessonLogEntry> lessonEntries = entries
                                .findByClassIdAndSemesterIdAndLessonDateBetweenOrderByLessonDateAscPeriodIndexAsc(
                                                classId, request.semesterId(), request.weekStart(),
                                                request.weekStart().plusDays(6));
                if (lessonEntries.isEmpty()) {
                        throw support.error(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                                        "Tuần chưa có tiết");
                }
                weeklyReviewService.lockEntries(lessonEntries, request.semesterId());
                List<LessonLogEntryResponse> resolvedEntries = scheduleService.classWeekItems(classId, semester,
                                request.weekStart(), request.weekStart().plusDays(6), lessonEntries, mapper,
                                rosterCounter, rubricMapper);
                support.ensureEntriesComplete(lessonEntries, resolvedEntries);
                LessonLogWeeklyReview review = weeklyReviewService.loadForUpdate(classId, request.semesterId(),
                                request.weekStart());
                support.assertVersion(review.getVersion(), request.expectedVersion());
                weeklySigningPolicy.validateExpectedEntries(review, request, lessonEntries);
                com.JavaTraining.BaiTap_RS.assignment.domain.entity.HomeroomAssignment homeroom = weeklySigningPolicy
                                .activeHomeroom(classId, request.weekStart());
                weeklySigningPolicy.validateHomeroomChange(review, homeroom, request);
                return signPersistence.saveSigned(review, homeroom, request, lessonEntries);
        }
}
