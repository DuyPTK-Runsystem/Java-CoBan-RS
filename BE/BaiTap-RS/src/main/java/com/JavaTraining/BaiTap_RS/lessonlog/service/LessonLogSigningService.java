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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LessonLogSigningService {
    private final LessonLogWeeklyCommandService weeklyCommandService;
    private final LessonLogResponseService responseService;
    private final LessonLogRosterService rosterService;

    public WeeklyReviewResponse signWeek(Long classId, ReqWeeklyReviewDTO request) {
        Function<LessonLogEntry, LessonLogEntryResponse> responseMapper = responseService::map;
        ToIntBiFunction<Long, LocalDateTime> rosterCounter = rosterService::count;
        Function<LessonLogPolicy, List<Object>> rubricMapper = responseService::rubric;
        return weeklyCommandService.signWeek(classId, request, responseMapper, rosterCounter, rubricMapper);
    }
}
