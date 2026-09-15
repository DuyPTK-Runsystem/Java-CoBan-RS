package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LessonLogSourceResolver {
    private final TimetableSourceLookup timetableLookup;
    private final AcademicLessonSourceValidator academicValidator;

    public LessonLogSource resolve(Long timetableEntryId, LocalDate lessonDate) {
        requireLessonDate(lessonDate);
        TimetableEntry timetable = timetableLookup.findEntry(timetableEntryId);
        TimetableRevision revision = timetableLookup.findRevision(timetable.getRevisionId());
        timetableLookup.lockAndValidateRevision(revision);
        timetableLookup.validateDate(timetable, revision, lessonDate);
        AcademicLessonSourceValidator.AcademicSource academic = academicValidator.resolve(timetable, revision,
                lessonDate);
        TimetablePeriod period = timetableLookup.findPeriod(timetable.getPeriodId());
        academicValidator.validatePeriodAndCalendar(academic, period, lessonDate);
        return new LessonLogSource(timetable, revision, academic.assignment(), academic.classId(), academic.subjectId(),
                academic.semester(), period);
    }

    public void lockForEntry(LessonLogEntry entry) {
        timetableLookup.lockForEntry(entry);
    }

    public void lockForEntries(List<LessonLogEntry> entries, Long semesterId) {
        timetableLookup.lockForEntries(entries, semesterId);
    }

    private void requireLessonDate(LocalDate lessonDate) {
        if (lessonDate == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Ngày học là bắt buộc");
        }
    }
}
