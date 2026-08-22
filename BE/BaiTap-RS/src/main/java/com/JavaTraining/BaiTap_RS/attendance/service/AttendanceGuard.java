package com.JavaTraining.BaiTap_RS.attendance.service;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.attendance.repository.AttendanceEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AttendanceGuard {

    private final SchoolClassRepository schoolClassRepository;
    private final SemesterRepository semesterRepository;
    private final TeacherRepository teacherRepository;
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;
    private final AttendanceEnrollmentRepository enrollmentRepository;

    public AttendanceGuard(
            SchoolClassRepository schoolClassRepository,
            SemesterRepository semesterRepository,
            TeacherRepository teacherRepository,
            HomeroomAssignmentRepository homeroomAssignmentRepository,
            AttendanceEnrollmentRepository enrollmentRepository) {
        this.schoolClassRepository = schoolClassRepository;
        this.semesterRepository = semesterRepository;
        this.teacherRepository = teacherRepository;
        this.homeroomAssignmentRepository = homeroomAssignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public SchoolClass findSchoolClass(Long classId) {
        return schoolClassRepository.findById(classId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp"));
    }

    public Semester findSemester(Long semesterId) {
        return semesterRepository.findById(semesterId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy học kỳ"));
    }

    public void validateClassSemesterAndDate(
            SchoolClass schoolClass,
            Semester semester,
            LocalDate attendanceDate) {
        if (!schoolClass.getAcademicYearId().equals(semester.getAcademicYearId())) {
            throw conflict("Lớp và học kỳ phải thuộc cùng năm học");
        }
        if (attendanceDate.isBefore(semester.getStartDate()) || attendanceDate.isAfter(semester.getEndDate())) {
            throw conflict("Ngày điểm danh phải nằm trong thời gian học kỳ");
        }
    }

    public void assertCurrentUserHomeroom(Long classId, LocalDate effectiveDate) {
        Long currentUserId = AuditContext.currentUserId();
        if (currentUserId == null) {
            throw forbidden();
        }
        Teacher teacher = teacherRepository.findByUserId(currentUserId)
                .orElseThrow(this::forbidden);
        boolean assigned = homeroomAssignmentRepository.existsActiveHomeroomAt(
                classId,
                teacher.getId(),
                AssignmentStatus.ACTIVE,
                effectiveDate);
        if (!assigned) {
            throw forbidden();
        }
    }

    public List<Student> findActiveClassStudents(Long classId, LocalDate effectiveDate) {
        return enrollmentRepository.findActiveStudentsInClassAt(
                classId,
                EnrollmentStatus.ACTIVE,
                effectiveDate.atStartOfDay(),
                effectiveDate.atTime(23, 59, 59));
    }

    public void assertStudentInClass(Long studentId, Long classId, LocalDate effectiveDate) {
        boolean enrolled = enrollmentRepository.existsActiveStudentInClassAt(
                studentId,
                classId,
                EnrollmentStatus.ACTIVE,
                effectiveDate.atStartOfDay(),
                effectiveDate.atTime(23, 59, 59));
        if (!enrolled) {
            throw conflict("Học sinh không thuộc lớp tại ngày điểm danh");
        }
    }

    public AppException notFound(String message) {
        return new AppException(HttpStatus.NOT_FOUND, message);
    }

    public AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }

    private AppException forbidden() {
        return new AppException(HttpStatus.FORBIDDEN, "GVCN chỉ được thao tác lớp được phân công");
    }
}
