package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.LocalDate;
import java.util.Objects;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableCalendarRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableClosedDateRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AcademicLessonSourceValidator {
    private final SubjectTeachingAssignmentRepository assignments;
    private final ClassSubjectRepository classSubjects;
    private final SemesterRepository semesters;
    private final TimetableCalendarRepository calendars;
    private final TimetableClosedDateRepository closedDates;

    public AcademicSource resolve(TimetableEntry timetable, TimetableRevision revision, LocalDate lessonDate) {
        SubjectTeachingAssignment assignment = assignments.findById(timetable.getAssignmentId())
                .orElseThrow(() -> error(HttpStatus.UNPROCESSABLE_ENTITY, "Phân công nguồn không tồn tại"));
        validateAssignment(assignment, lessonDate);
        ClassSubject classSubject = classSubjects.findById(assignment.getClassSubjectId())
                .orElseThrow(() -> error(HttpStatus.UNPROCESSABLE_ENTITY, "Môn lớp nguồn không tồn tại"));
        if (!Objects.equals(classSubject.getSemesterId(), revision.getSemesterId())) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "Môn lớp không thuộc học kỳ của revision");
        }
        Semester semester = semesters.findById(revision.getSemesterId())
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Không tìm thấy học kỳ"));
        if (lessonDate.isBefore(semester.getStartDate()) || lessonDate.isAfter(semester.getEndDate())) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "Ngày không thuộc học kỳ của revision");
        }
        return new AcademicSource(assignment, classSubject.getClassId(), classSubject.getSubjectId(), semester);
    }

    public void validatePeriodAndCalendar(AcademicSource source, TimetablePeriod period, LocalDate lessonDate) {
        if (period.getDayOfWeek() != lessonDate.getDayOfWeek().getValue()) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "Ngày không đúng thứ của tiết");
        }
        if (calendars.findBySemesterId(source.semester().getId())
                .map(calendar -> closedDates.existsByCalendarIdAndClosedDate(calendar.getId(), lessonDate))
                .orElse(false)) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "Ngày học đã đóng");
        }
    }

    private void validateAssignment(SubjectTeachingAssignment assignment, LocalDate lessonDate) {
        if (assignment.getStatus() != AssignmentStatus.ACTIVE || lessonDate.isBefore(assignment.getValidFrom())
                || (assignment.getValidTo() != null && lessonDate.isAfter(assignment.getValidTo()))) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "Phân công không ACTIVE hoặc không còn hiệu lực");
        }
    }

    private AppException error(HttpStatus status, String message) {
        return new AppException(status, message);
    }

    public record AcademicSource(SubjectTeachingAssignment assignment, Long classId, Long subjectId,
            Semester semester) {
    }
}
