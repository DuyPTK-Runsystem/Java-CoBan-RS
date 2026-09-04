package com.JavaTraining.BaiTap_RS.assignment.service;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.ClassSubjectStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.ClassSubjectRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.SubjectTeachingAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.SubjectTeachingAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.TeacherStatus;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SubjectTeachingAssignmentGuard {

    private final ClassSubjectRepository classSubjectRepository;
    private final SemesterRepository semesterRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectTeachingAssignmentRepository subjectTeachingRepository;

    public SubjectTeachingAssignmentGuard(
            ClassSubjectRepository classSubjectRepository,
            SemesterRepository semesterRepository,
            TeacherRepository teacherRepository,
            SubjectTeachingAssignmentRepository subjectTeachingRepository) {
        this.classSubjectRepository = classSubjectRepository;
        this.semesterRepository = semesterRepository;
        this.teacherRepository = teacherRepository;
        this.subjectTeachingRepository = subjectTeachingRepository;
    }

    public void validateWindowInSemester(ClassSubject classSubject, LocalDate validFrom, LocalDate validTo) {
        validateWindow(validFrom, validTo);
        if (classSubject.getStatus() != ClassSubjectStatus.ACTIVE) {
            throw conflict("Lớp-môn chưa ACTIVE");
        }
        Semester semester = semesterRepository.findById(classSubject.getSemesterId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy học kỳ"));
        LocalDate effectiveEnd = validTo == null ? semester.getEndDate() : validTo;
        validateSemesterBoundary(semester, validFrom, effectiveEnd);
    }

    private void validateSemesterBoundary(Semester semester, LocalDate validFrom, LocalDate effectiveEnd) {
        if (validFrom.isBefore(semester.getStartDate()) || effectiveEnd.isAfter(semester.getEndDate())) {
            throw conflict("Phân công GVBM phải nằm trong học kỳ");
        }
    }

    public Teacher findActiveTeacher(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy giáo viên"));
        if (teacher.getStatus() != TeacherStatus.ACTIVE) {
            throw conflict("Chỉ giáo viên ACTIVE mới được nhận phân công mới");
        }
        return teacher;
    }

    public ClassSubject findClassSubject(Long classSubjectId) {
        return classSubjectRepository.findById(classSubjectId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp-môn"));
    }

    public ClassSubject lockClassSubject(Long classSubjectId) {
        return classSubjectRepository.findByIdForUpdate(classSubjectId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp-môn"));
    }

    public SubjectTeachingAssignment findSubjectTeachingAssignment(Long assignmentId) {
        return subjectTeachingRepository.findById(assignmentId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy phân công GVBM"));
    }

    public AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }

    private void validateWindow(LocalDate validFrom, LocalDate validTo) {
        if (validTo != null && validTo.isBefore(validFrom)) {
            throw conflict("Ngày kết thúc không được trước ngày bắt đầu");
        }
    }
}
