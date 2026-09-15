package com.JavaTraining.BaiTap_RS.lessonlog.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Immutable values required to create an entry from a timetable source. */
public record LessonLogEntrySourceData(Long timetableEntryId, Long revisionId, Long assignmentId, Long semesterId,
        Long classId, Long subjectId, Long teacherId, Long policyId, LocalDate date, String session,
        Integer periodIndex, String snapshot, LocalDateTime ends, LocalDateTime expires, Long actor) {
}
