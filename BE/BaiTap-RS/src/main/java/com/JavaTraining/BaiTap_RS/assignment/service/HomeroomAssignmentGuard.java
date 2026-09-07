package com.JavaTraining.BaiTap_RS.assignment.service;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.assignment.domain.entity.HomeroomAssignment;
import com.JavaTraining.BaiTap_RS.assignment.repository.HomeroomAssignmentRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.TeacherStatus;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class HomeroomAssignmentGuard {

    private final SchoolClassRepository schoolClassRepository;
    private final AcademicYearRepository academicYearRepository;
    private final TeacherRepository teacherRepository;
    private final HomeroomAssignmentRepository homeroomRepository;

    public HomeroomAssignmentGuard(
            SchoolClassRepository schoolClassRepository,
            AcademicYearRepository academicYearRepository,
            TeacherRepository teacherRepository,
            HomeroomAssignmentRepository homeroomRepository) {
        this.schoolClassRepository = schoolClassRepository;
        this.academicYearRepository = academicYearRepository;
        this.teacherRepository = teacherRepository;
        this.homeroomRepository = homeroomRepository;
    }

    public void validateWindowInYear(SchoolClass schoolClass, LocalDate validFrom, LocalDate validTo) {
        if (validTo != null && validTo.isBefore(validFrom)) {
            throw conflict("Ngày kết thúc không được trước ngày bắt đầu");
        }
        AcademicYear year = academicYearRepository.findById(schoolClass.getAcademicYearId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy năm học"));
        LocalDate effectiveEnd = validTo == null ? year.getEndDate() : validTo;
        if (validFrom.isBefore(year.getStartDate()) || effectiveEnd.isAfter(year.getEndDate())) {
            throw conflict("Phân công GVCN phải nằm trong năm học");
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

    public SchoolClass findSchoolClass(Long classId) {
        return schoolClassRepository.findById(classId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp"));
    }

    public SchoolClass lockSchoolClass(Long classId) {
        return schoolClassRepository.findByIdForUpdate(classId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp"));
    }

    public HomeroomAssignment findHomeroomAssignment(Long assignmentId) {
        return homeroomRepository.findById(assignmentId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy phân công GVCN"));
    }

    public AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}
