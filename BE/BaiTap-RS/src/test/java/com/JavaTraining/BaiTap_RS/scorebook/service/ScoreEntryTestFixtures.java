package com.JavaTraining.BaiTap_RS.scorebook.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ApplicationScope;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectType;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumn;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentColumnStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.AssessmentType;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScoreStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.Scorebook;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.ScorebookStatus;
import com.JavaTraining.BaiTap_RS.scorebook.domain.entity.StudentScore;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.domain.entity.StudentStatus;
import org.springframework.test.util.ReflectionTestUtils;

final class ScoreEntryTestFixtures {

    private ScoreEntryTestFixtures() {
    }

    /* default */ static Scorebook scorebook(Long id, Long classSubjectId, ScorebookStatus status) {
        Scorebook scorebook = new Scorebook(classSubjectId, status);
        ReflectionTestUtils.setField(scorebook, "id", id);
        return scorebook;
    }

    /* default */ static ClassSubject classSubject(Long id, Long classId, Long subjectId, Long semesterId) {
        ClassSubject classSubject = new ClassSubject(classId, subjectId, semesterId, ClassSubjectStatus.ACTIVE);
        ReflectionTestUtils.setField(classSubject, "id", id);
        return classSubject;
    }

    /* default */ static Subject subject(Long id) {
        Subject subject = new Subject("MATH", "Toán học", SubjectType.ACADEMIC, ApplicationScope.CLASS,
                SubjectStatus.ACTIVE);
        ReflectionTestUtils.setField(subject, "id", id);
        return subject;
    }

    /* default */ static Semester semester(Long id, Long academicYearId, SemesterStatus status) {
        Semester semester = new Semester(
                academicYearId,
                "HK1",
                "Học kỳ 1",
                1,
                LocalDate.of(2026, 8, 15),
                LocalDate.of(2026, 12, 31),
                null,
                status);
        ReflectionTestUtils.setField(semester, "id", id);
        return semester;
    }

    /* default */ static AssessmentColumn column(Long id, Long scorebookId, AssessmentType type,
            AssessmentColumnStatus status) {
        AssessmentColumn column = new AssessmentColumn(
                scorebookId,
                type,
                1,
                type.name(),
                type.standardWeight(),
                type.isRequiredByStructure());
        ReflectionTestUtils.setField(column, "id", id);
        column.setStatus(status);
        return column;
    }

    /* default */ static Student student(Long id, String code, StudentStatus status) {
        Student student = new Student("Học sinh " + id, code);
        student.setStatus(status);
        ReflectionTestUtils.setField(student, "id", id);
        return student;
    }

    /* default */ static StudentYearEnrollment enrollment(Long id, Long studentId, Long academicYearId, Long classId,
            EnrollmentStatus status) {
        StudentYearEnrollment enrollment = new StudentYearEnrollment(
                studentId, academicYearId, classId, status, LocalDateTime.now());
        ReflectionTestUtils.setField(enrollment, "id", id);
        return enrollment;
    }

    /* default */ static StudentScore score(
            Long id,
            Long columnId,
            Long studentId,
            ScoreStatus status,
            BigDecimal value,
            Long version,
            LocalDateTime enteredAt) {
        StudentScore score = new StudentScore(columnId, studentId, status, value, "Ghi chú", 1L);
        ReflectionTestUtils.setField(score, "id", id);
        ReflectionTestUtils.setField(score, "version", version);
        ReflectionTestUtils.setField(score, "enteredAt", enteredAt);
        return score;
    }
}
