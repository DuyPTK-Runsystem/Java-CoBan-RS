package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;

import org.springframework.stereotype.Service;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogCalendarDayResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogEntryResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogScheduleResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogPolicy;

import lombok.RequiredArgsConstructor;

/** Coordinates schedule reads while builders and week resolution own details. */
@Service
@RequiredArgsConstructor
public class LessonLogScheduleService {
    private final LessonLogTeacherScheduleService teacherScheduleService;
    private final LessonLogScheduleCalendarCatalog calendarCatalog;
    private final LessonLogScheduleWeekService weekService;

    public LessonLogScheduleResponse teacherSchedule(LocalDate date, Long teacherId,
            Function<LessonLogEntry, LessonLogEntryResponse> mapper,
            ToIntBiFunction<Long, LocalDateTime> rosterCounter) {
        return teacherScheduleService.schedule(date, teacherId, mapper, rosterCounter);
    }

    public List<LessonLogEntryResponse> classWeekItems(Long classId, Semester semester, LocalDate from, LocalDate to,
            List<LessonLogEntry> logged, Function<LessonLogEntry, LessonLogEntryResponse> mapper,
            ToIntBiFunction<Long, LocalDateTime> rosterCounter, Function<LessonLogPolicy, List<Object>> rubricMapper) {
        return weekService.items(classId, semester, from, to, logged, mapper, rosterCounter, rubricMapper);
    }

    public List<LessonLogCalendarDayResponse> calendarDays(Semester semester, LocalDate start) {
        return calendarCatalog.calendarDays(semester, start);
    }

    public String roomName(Long roomId) {
        return calendarCatalog.roomName(roomId);
    }

}
