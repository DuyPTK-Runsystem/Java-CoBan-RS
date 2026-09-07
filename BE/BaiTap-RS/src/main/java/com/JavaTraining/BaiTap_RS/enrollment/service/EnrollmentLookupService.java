package com.JavaTraining.BaiTap_RS.enrollment.service;

import java.util.Collection;
import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.GradeLevel;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.GradeLevelRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import com.JavaTraining.BaiTap_RS.student.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class EnrollmentLookupService {

    private final StudentRepository studentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentYearEnrollmentRepository enrollmentRepository;

    public EnrollmentLookupService(
            StudentRepository studentRepository,
            AcademicYearRepository academicYearRepository,
            GradeLevelRepository gradeLevelRepository,
            SchoolClassRepository schoolClassRepository,
            StudentYearEnrollmentRepository enrollmentRepository) {
        this.studentRepository = studentRepository;
        this.academicYearRepository = academicYearRepository;
        this.gradeLevelRepository = gradeLevelRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public Student findStudent(Long studentId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                EnrollmentLookupService.class,
                "EnrollmentLookupService.findStudent");
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy học sinh"));
    }

    public List<Student> findStudents(Collection<Long> studentIds) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                EnrollmentLookupService.class,
                "EnrollmentLookupService.findStudents");
        List<Student> students = studentRepository.findAllById(studentIds);
        if (students.size() != studentIds.size()) {
            throw new AppException(HttpStatus.NOT_FOUND, "Có học sinh không tồn tại");
        }
        return students;
    }

    public AcademicYear findAcademicYear(Long academicYearId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                EnrollmentLookupService.class,
                "EnrollmentLookupService.findAcademicYear");
        return academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy năm học"));
    }

    public SchoolClass findSchoolClass(Long classId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                EnrollmentLookupService.class,
                "EnrollmentLookupService.findSchoolClass");
        return schoolClassRepository.findById(classId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp"));
    }

    public GradeLevel findGradeForClass(SchoolClass schoolClass) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                EnrollmentLookupService.class,
                "EnrollmentLookupService.findGradeForClass");
        return gradeLevelRepository.findById(schoolClass.getGradeLevelId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khối của lớp"));
    }

    public StudentYearEnrollment findEnrollment(Long enrollmentId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                EnrollmentLookupService.class,
                "EnrollmentLookupService.findEnrollment");
        return enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy enrollment"));
    }
}
