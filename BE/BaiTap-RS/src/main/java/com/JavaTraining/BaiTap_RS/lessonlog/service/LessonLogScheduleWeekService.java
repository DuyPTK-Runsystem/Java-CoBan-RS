package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogEntryResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogPolicy;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogEntryRepository;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;

import lombok.RequiredArgsConstructor;

/** Resolves the effective timetable occurrences for a class week. */
@Service
@RequiredArgsConstructor
public class LessonLogScheduleWeekService {
    private final LessonLogScheduleTimetableCatalog timetableCatalog;
    private final LessonLogScheduleAcademicCatalog academicCatalog;
    private final LessonLogScheduleEntryFactory entryFactory;
    private final LessonLogEntryRepository lessonEntries;

    public List<LessonLogEntryResponse> items(Long classId, Semester semester, LocalDate from, LocalDate to,
            List<LessonLogEntry> logged, Function<LessonLogEntry, LessonLogEntryResponse> mapper,
            ToIntBiFunction<Long, LocalDateTime> rosterCounter, Function<LessonLogPolicy, List<Object>> rubricMapper) {
        Map<String, LessonLogEntryResponse> resolved = new LinkedHashMap<>();
        Set<String> effectiveOccurrences = new HashSet<>();
        logged.forEach(entry -> resolved.put(identity(entry), mapper.apply(entry)));
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (date.isBefore(semester.getStartDate()) || date.isAfter(semester.getEndDate())) {
                continue;
            }
            addEffectiveEntries(classId, semester, date, resolved, effectiveOccurrences, rosterCounter, rubricMapper);
        }
        return resolved.values().stream()
                .sorted(java.util.Comparator.comparing(LessonLogEntryResponse::lessonDate)
                        .thenComparing(LessonLogEntryResponse::session)
                        .thenComparing(LessonLogEntryResponse::periodIndex))
                .toList();
    }

    private void addEffectiveEntries(Long classId, Semester semester, LocalDate date,
            Map<String, LessonLogEntryResponse> resolved, Set<String> effectiveOccurrences,
            ToIntBiFunction<Long, LocalDateTime> rosterCounter, Function<LessonLogPolicy, List<Object>> rubricMapper) {
        for (TimetableRevision revision : timetableCatalog.effectiveRevisions(semester.getId(), date)) {
            for (TimetableEntry timetableEntry : timetableCatalog.entriesFor(revision.getId())) {
                addEntry(classId, semester, date, revision, timetableEntry, resolved, effectiveOccurrences,
                        rosterCounter, rubricMapper);
            }
        }
    }

    private void addEntry(Long classId, Semester semester, LocalDate date, TimetableRevision revision,
            TimetableEntry timetableEntry, Map<String, LessonLogEntryResponse> resolved,
            Set<String> effectiveOccurrences, ToIntBiFunction<Long, LocalDateTime> rosterCounter,
            Function<LessonLogPolicy, List<Object>> rubricMapper) {
        TimetablePeriod period = timetableCatalog.period(timetableEntry.getPeriodId()).orElse(null);
        SubjectTeachingAssignment assignment = timetableCatalog.assignment(timetableEntry.getAssignmentId()).orElse(null);
        if (!matchesDate(date, period, assignment, timetableEntry)) {
            return;
        }
        ClassSubject classSubject = academicCatalog.classSubject(assignment.getClassSubjectId()).orElse(null);
        if (classSubject == null || !java.util.Objects.equals(classSubject.getSemesterId(), semester.getId())
                || !java.util.Objects.equals(classSubject.getClassId(), classId)) {
            return;
        }
        String key = identity(date, period.getSession().name(), period.getPeriodIndex());
        if (!effectiveOccurrences.add(key)) {
            throw error("Có nhiều tiết thời khóa biểu cùng hiệu lực cho một ô tuần");
        }
        if (!resolved.containsKey(key)) {
            resolved.put(key, entryFactory.unlogged(timetableEntry, revision, assignment, classSubject, semester, period,
                    date, rosterCounter, rubricMapper));
        }
    }

    private boolean matchesDate(LocalDate date, TimetablePeriod period, SubjectTeachingAssignment assignment,
            TimetableEntry timetableEntry) {
        return period != null && assignment != null && period.getDayOfWeek() == date.getDayOfWeek().getValue()
                && !date.isBefore(timetableEntry.getValidFrom())
                && !date.isAfter(timetableEntry.getValidTo()) && !date.isBefore(assignment.getValidFrom())
                && (assignment.getValidTo() == null || !date.isAfter(assignment.getValidTo()));
    }

    private String identity(LessonLogEntry entry) {
        return identity(entry.getLessonDate(), entry.getSession(), entry.getPeriodIndex());
    }

    private String identity(LocalDate date, String session, Integer period) {
        return date + "|" + session + "|" + period;
    }

    private AppException error(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}
