package com.JavaTraining.BaiTap_RS.attendance.service;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.AssignmentStatus;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.attendance.domain.entity.AttendanceSessionPeriod;
import com.JavaTraining.BaiTap_RS.attendance.repository.AttendanceEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.calendar.service.CalendarValidityService;
import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings({"PMD.GuardLogStatement", "PMD.TooManyMethods"})
public class AttendanceGuard {

    private final SchoolClassRepository schoolClassRepository;
    private final SemesterRepository semesterRepository;
    private final TeacherRepository teacherRepository;
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;
    private final AttendanceEnrollmentRepository enrollmentRepository;
    private final CalendarValidityService calendarValidityService;

    public AttendanceGuard(
            SchoolClassRepository schoolClassRepository,
            SemesterRepository semesterRepository,
            TeacherRepository teacherRepository,
            HomeroomAssignmentRepository homeroomAssignmentRepository,
            AttendanceEnrollmentRepository enrollmentRepository,
            CalendarValidityService calendarValidityService) {
        this.schoolClassRepository = schoolClassRepository;
        this.semesterRepository = semesterRepository;
        this.teacherRepository = teacherRepository;
        this.homeroomAssignmentRepository = homeroomAssignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.calendarValidityService = calendarValidityService;
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
            LocalDate attendanceDate,
            AttendanceSessionPeriod sessionPeriod) {
        validateClassAndSemester(schoolClass, semester);
        if (attendanceDate.isBefore(semester.getStartDate()) || attendanceDate.isAfter(semester.getEndDate())) {
            throw new AppException(HttpStatus.CONFLICT, "Ngày điểm danh phải nằm trong thời gian học kỳ");
        }
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                AttendanceGuard.class,
                "AttendanceGuard.validateClassSemesterAndDate",
                "calendar validation classId={}, semesterId={}, academicYearId={}, date={}, period={}",
                schoolClass.getId(), semester.getId(), semester.getAcademicYearId(), attendanceDate, sessionPeriod);
        calendarValidityService.assertScheduled(semester.getId(), attendanceDate, sessionPeriod);
    }

    public void validateClassSemesterAndDateForOffice(
            SchoolClass schoolClass,
            Semester semester,
            LocalDate attendanceDate,
            AttendanceSessionPeriod sessionPeriod) {
        validateClassAndSemester(schoolClass, semester);
        validateAttendanceDate(semester, attendanceDate);
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                AttendanceGuard.class,
                "AttendanceGuard.validateClassSemesterAndDateForOffice",
                "calendar ensure classId={}, semesterId={}, academicYearId={}, date={}, period={}",
                schoolClass.getId(), semester.getId(), semester.getAcademicYearId(), attendanceDate, sessionPeriod);
        calendarValidityService.ensureScheduled(semester.getId(), attendanceDate, sessionPeriod);
    }

    private void validateAttendanceDate(Semester semester, LocalDate attendanceDate) {
        if (attendanceDate.isBefore(semester.getStartDate()) || attendanceDate.isAfter(semester.getEndDate())) {
            throw new AppException(HttpStatus.CONFLICT, "Ngày điểm danh phải nằm trong thời gian học kỳ");
        }
    }

    public void validateClassAndSemester(SchoolClass schoolClass, Semester semester) {
        if (!schoolClass.getAcademicYearId().equals(semester.getAcademicYearId())) {
            throw new AppException(HttpStatus.CONFLICT, "Lớp và học kỳ phải thuộc cùng năm học");
        }
    }

    public void assertCurrentUserHomeroom(Long classId, LocalDate effectiveDate) {
        validateCurrentUserHomeroomInRange(classId, effectiveDate, effectiveDate);
    }

    public void validateCurrentUserHomeroomInRange(Long classId, LocalDate from, LocalDate to) {
        Long currentUserId = AuditContext.currentUserId();
        if (currentUserId == null) {
            throw new AppException(HttpStatus.FORBIDDEN, "GVCN chỉ được thao tác lớp được phân công");
        }
        Teacher teacher = teacherRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new AppException(HttpStatus.FORBIDDEN, "GVCN chỉ được thao tác lớp được phân công"));
        boolean assigned = homeroomAssignmentRepository.existsActiveHomeroomBetween(
                classId,
                teacher.getId(),
                AssignmentStatus.ACTIVE,
                from,
                to);
        if (!assigned) {
            throw new AppException(HttpStatus.FORBIDDEN, "GVCN chỉ được thao tác lớp được phân công");
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
            throw new AppException(HttpStatus.CONFLICT, "Học sinh không thuộc lớp tại ngày điểm danh");
        }
    }

    public AppException notFound(String message) {
        return new AppException(HttpStatus.NOT_FOUND, message);
    }
}
