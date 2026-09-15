package com.JavaTraining.BaiTap_RS.lessonlog.service;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;

public record LessonLogSource(
        TimetableEntry timetable,
        TimetableRevision revision,
        SubjectTeachingAssignment assignment,
        Long classId,
        Long subjectId,
        Semester semester,
        TimetablePeriod period) {
}
