package com.JavaTraining.BaiTap_RS.lessonlog.service;

import org.springframework.stereotype.Service;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogPolicy;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LessonLogSnapshotService {
    private final SchoolClassRepository schoolClasses;
    private final SubjectRepository subjects;
    private final TeacherRepository teachers;
    private final LessonLogScheduleService scheduleService;

    public String create(LessonLogSource source, LessonLogPolicy policy, java.time.LocalDate lessonDate,
            int rosterSnapshot) {
        return "{\"timetableEntryId\":" + source.timetable().getId() + ",\"revisionId\":" + source.revision().getId()
                + ",\"revisionStatus\":" + quote(source.revision().getStatus().name()) + ",\"assignmentId\":"
                + source.assignment().getId() + ",\"classId\":" + source.classId() + ",\"subjectId\":" + source.subjectId()
                + ",\"teacherId\":" + source.assignment().getTeacherId() + ",\"semesterId\":" + source.semester().getId()
                + ",\"className\":" + quote(className(source.classId())) + ",\"subjectName\":"
                + quote(subjectName(source.subjectId())) + ",\"teacherName\":" + quote(teacherName(source.assignment().getTeacherId()))
                + ",\"functionalRoomName\":" + quote(scheduleService.roomName(source.timetable().getFunctionalRoomId()))
                + ",\"lessonDate\":" + quote(lessonDate.toString()) + ",\"session\":" + quote(source.period().getSession().name())
                + ",\"periodIndex\":" + source.period().getPeriodIndex() + ",\"startTime\":"
                + quote(source.period().getStartTime().toString()) + ",\"endTime\":" + quote(source.period().getEndTime().toString())
                + ",\"policyId\":" + policy.getId() + ",\"policyVersion\":" + policy.getPolicyVersion() + ",\"deadlineMode\":"
                + quote(policy.getDeadlineMode()) + ",\"editWindowHours\":"
                + (policy.getEditWindowHours() == null ? "null" : policy.getEditWindowHours()) + ",\"rosterCountSnapshot\":"
                + rosterSnapshot + "}";
    }

    private String quote(String value) {
        return value == null ? "null" : "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String className(Long classId) {
        return schoolClasses.findById(classId).map(SchoolClass::getClassName).orElse(null);
    }

    private String subjectName(Long subjectId) {
        return subjects.findById(subjectId).map(Subject::getName).orElse(null);
    }

    private String teacherName(Long teacherId) {
        return teachers.findById(teacherId)
                .map(com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher::getTeacherName).orElse(null);
    }
}
