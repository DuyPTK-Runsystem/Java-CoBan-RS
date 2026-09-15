package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevisionStatus;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableEntryRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetablePeriodRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableRevisionRepository;

import lombok.RequiredArgsConstructor;

/** Reads timetable records relevant to a lesson-log occurrence. */
@Service
@RequiredArgsConstructor
public class LessonLogScheduleTimetableCatalog {
    private final SemesterRepository semesters;
    private final TimetableRevisionRepository revisions;
    private final TimetableEntryRepository entries;
    private final TimetablePeriodRepository periods;
    private final SubjectTeachingAssignmentRepository assignments;

    public List<Semester> activeOn(LocalDate date) {
        return semesters.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date);
    }

    public List<TimetableRevision> effectiveRevisions(Long semesterId, LocalDate date) {
        return revisions.findEffectiveBySemesterAndDate(semesterId, date,
                List.of(TimetableRevisionStatus.PUBLISHED, TimetableRevisionStatus.ARCHIVED));
    }

    public List<TimetableEntry> entriesFor(Long revisionId) {
        return entries.findByRevisionId(revisionId);
    }

    public Optional<TimetablePeriod> period(Long periodId) {
        return periods.findById(periodId);
    }

    public Optional<SubjectTeachingAssignment> assignment(Long assignmentId) {
        return assignments.findById(assignmentId);
    }
}
