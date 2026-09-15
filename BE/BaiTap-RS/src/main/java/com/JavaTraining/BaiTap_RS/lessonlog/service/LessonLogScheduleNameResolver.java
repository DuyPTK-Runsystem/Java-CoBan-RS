package com.JavaTraining.BaiTap_RS.lessonlog.service;

import org.springframework.stereotype.Service;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.FunctionalRoom;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;

import lombok.RequiredArgsConstructor;

/** Resolves display names needed by lesson-log schedule responses. */
@Service
@RequiredArgsConstructor
public class LessonLogScheduleNameResolver {
    private final LessonLogScheduleAcademicCatalog academicCatalog;
    private final LessonLogScheduleCalendarCatalog calendarCatalog;

    public String className(ClassSubject classSubject) {
        return academicCatalog.schoolClass(classSubject.getClassId()).map(SchoolClass::getClassName).orElse(null);
    }

    public String subjectName(ClassSubject classSubject) {
        return academicCatalog.subject(classSubject.getSubjectId()).map(Subject::getName).orElse(null);
    }

    public String teacherName(SubjectTeachingAssignment assignment) {
        return academicCatalog.teacher(assignment.getTeacherId()).map(Teacher::getTeacherName).orElse(null);
    }

    public String roomName(TimetableEntry timetableEntry) {
        return calendarCatalog.room(timetableEntry.getFunctionalRoomId()).map(FunctionalRoom::getName).orElse(null);
    }
}
