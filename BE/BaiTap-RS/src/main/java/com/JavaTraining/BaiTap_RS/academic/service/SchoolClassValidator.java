package com.JavaTraining.BaiTap_RS.academic.service;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.GradeLevel;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.repository.ClassTransferHistoryRepository;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SchoolClassValidator {

    private final SchoolClassRepository schoolClassRepository;
    private final StudentYearEnrollmentRepository enrollmentRepository;
    private final ClassTransferHistoryRepository historyRepository;

    public SchoolClassValidator(
            SchoolClassRepository schoolClassRepository,
            StudentYearEnrollmentRepository enrollmentRepository,
            ClassTransferHistoryRepository historyRepository) {
        this.schoolClassRepository = schoolClassRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.historyRepository = historyRepository;
    }

    public void validateClassCreation(AcademicYear year, GradeLevel grade, SchoolClassStatus status) {
        if (!grade.isActive() && status == SchoolClassStatus.ACTIVE) {
            throw conflict("Khối đã ngừng hoạt động");
        }
        if (status == SchoolClassStatus.ACTIVE && year.getStatus() != AcademicYearStatus.ACTIVE) {
            throw conflict("Chỉ năm học ACTIVE mới nhận lớp ACTIVE");
        }
        if (year.getStatus() == AcademicYearStatus.CLOSED) {
            throw conflict("Năm học đã đóng");
        }
    }

    public void rejectDuplicateClassCode(Long academicYearId, String classCode, Long id) {
        boolean duplicate = id == null
                ? schoolClassRepository.existsByAcademicYearIdAndClassCode(academicYearId, classCode)
                : schoolClassRepository.existsByAcademicYearIdAndClassCodeAndIdNot(academicYearId, classCode, id);
        if (duplicate) {
            throw conflict("Mã lớp đã tồn tại trong năm học");
        }
    }

    public void rejectGradeChangeWithEnrollment(Long classId, Long requestGradeId, Long currentGradeId) {
        if (enrollmentRepository.existsByCurrentClassId(classId)
                && !requestGradeId.equals(currentGradeId)) {
            throw conflict("Không thể đổi khối của lớp đã có học sinh");
        }
    }

    public void ensureClassNotClosed(SchoolClass schoolClass) {
        if (schoolClass.getStatus() == SchoolClassStatus.CLOSED) {
            throw conflict("Lớp đã đóng");
        }
    }

    public void validateDelete(Long id) {
        if (enrollmentRepository.existsByCurrentClassId(id)
                || historyRepository.existsByFromClassId(id)
                || historyRepository.existsByToClassId(id)) {
            throw conflict("Không thể xóa lớp đã phát sinh enrollment");
        }
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}
