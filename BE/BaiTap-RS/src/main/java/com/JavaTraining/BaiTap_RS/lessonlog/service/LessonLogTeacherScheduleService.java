package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;

import org.springframework.stereotype.Service;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogEntryResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogScheduleItemResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogScheduleResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogEntryRepository;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LessonLogTeacherScheduleService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final LessonLogScheduleTimetableCatalog timetableCatalog;
    private final LessonLogScheduleAcademicCatalog academicCatalog;
    private final LessonLogScheduleEntryFactory entryFactory;
    private final LessonLogEntryRepository lessonEntries;

    public LessonLogScheduleResponse schedule(LocalDate date, Long teacherId,
            Function<LessonLogEntry, LessonLogEntryResponse> mapper, ToIntBiFunction<Long, LocalDateTime> rosterCounter) {
        List<LessonLogScheduleItemResponse> scheduleItems = new ArrayList<>();
        for (Semester semester : timetableCatalog.activeOn(date)) {
            addSemester(date, teacherId, mapper, rosterCounter, scheduleItems, semester);
        }
        scheduleItems.sort(Comparator.comparing(LessonLogScheduleItemResponse::lessonDate)
                .thenComparing(LessonLogScheduleItemResponse::session)
                .thenComparing(LessonLogScheduleItemResponse::periodIndex));
        return new LessonLogScheduleResponse(date, ZONE.getId(), scheduleItems);
    }

    private void addSemester(LocalDate date, Long teacherId, Function<LessonLogEntry, LessonLogEntryResponse> mapper,
            ToIntBiFunction<Long, LocalDateTime> rosterCounter, List<LessonLogScheduleItemResponse> scheduleItems,
            Semester semester) {
        for (TimetableRevision revision : timetableCatalog.effectiveRevisions(semester.getId(), date)) {
            for (TimetableEntry timetableEntry : timetableCatalog.entriesFor(revision.getId())) {
                addEntry(date, teacherId, mapper, rosterCounter, scheduleItems, semester, revision, timetableEntry);
            }
        }
    }

    private void addEntry(LocalDate date, Long teacherId, Function<LessonLogEntry, LessonLogEntryResponse> mapper,
            ToIntBiFunction<Long, LocalDateTime> rosterCounter, List<LessonLogScheduleItemResponse> scheduleItems,
            Semester semester, TimetableRevision revision, TimetableEntry timetableEntry) {
        TimetablePeriod period = timetableCatalog.period(timetableEntry.getPeriodId()).orElse(null);
        SubjectTeachingAssignment assignment = timetableCatalog.assignment(timetableEntry.getAssignmentId()).orElse(null);
        if (!matchesTeacher(teacherId, date, period, assignment, timetableEntry)) {
            return;
        }
        ClassSubject classSubject = academicCatalog.classSubject(assignment.getClassSubjectId()).orElse(null);
        if (classSubject == null || !Objects.equals(classSubject.getSemesterId(), semester.getId())) {
            return;
        }
        LessonLogEntry logged = lessonEntries.findByClassIdAndLessonDateAndSessionAndPeriodIndex(classSubject.getClassId(),
                date, period.getSession().name(), period.getPeriodIndex()).orElse(null);
        scheduleItems.add(entryFactory.scheduleItem(timetableEntry, revision, assignment, classSubject, semester, period,
                logged, date, mapper, rosterCounter));
    }

    private boolean matchesTeacher(Long teacherId, LocalDate date, TimetablePeriod period,
            SubjectTeachingAssignment assignment, TimetableEntry timetableEntry) {
        return period != null && assignment != null && period.getDayOfWeek() == date.getDayOfWeek().getValue()
                && Objects.equals(assignment.getTeacherId(), teacherId) && !date.isBefore(timetableEntry.getValidFrom())
                && !date.isAfter(timetableEntry.getValidTo()) && !date.isBefore(assignment.getValidFrom())
                && (assignment.getValidTo() == null || !date.isAfter(assignment.getValidTo()));
    }
}
