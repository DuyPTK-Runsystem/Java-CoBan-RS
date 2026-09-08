package com.JavaTraining.BaiTap_RS.enrollment.service;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.domain.DTOs.requests.ReqTransferWithScoresDTO;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.StudentYearEnrollment;
import com.JavaTraining.BaiTap_RS.student.domain.entity.Student;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class EnrollmentTransferScoreContextService {

    private final EnrollmentLookupService lookupService;
    private final EnrollmentTransferScoreDataLoader dataLoader;
    private final EnrollmentTransferScoreReferenceService referenceService;

    public EnrollmentTransferScoreContextService(
            EnrollmentLookupService lookupService,
            EnrollmentTransferScoreDataLoader dataLoader,
            EnrollmentTransferScoreReferenceService referenceService) {
        this.lookupService = lookupService;
        this.dataLoader = dataLoader;
        this.referenceService = referenceService;
    }

    public TransferScoreContext loadContext(Long enrollmentId, Long targetClassId, Long semesterId) {
        StudentYearEnrollment enrollment = lookupService.findEnrollment(enrollmentId);
        AcademicYear year = loadAndValidateYear(enrollment);
        SchoolClass sourceClass = lookupService.findSchoolClass(enrollment.getCurrentClassId());
        SchoolClass targetClass = validateTargetClass(targetClassId, year);
        validateDifferentClasses(sourceClass, targetClass);
        Semester semester = semesterId == null ? null : referenceService.findSemester(semesterId);
        validateSemester(year, semester);
        Student student = lookupService.findStudent(enrollment.getStudentId());
        return new TransferScoreContext(enrollment, year, sourceClass, targetClass, semester, student);
    }

    public Semester resolveSemesterForMutation(Long enrollmentId, ReqTransferWithScoresDTO request) {
        if (request.scores().isEmpty()) {
            throw badRequest("Transfer-with-scores phải có ít nhất một cột điểm");
        }
        Long semesterId = request.scores().stream()
                .map(score -> referenceService.scoreDataColumnSemester(score.assessmentColumnId()))
                .findFirst()
                .orElseThrow(() -> conflict("Không xác định được học kỳ của cột điểm đích"));
        return loadContext(enrollmentId, request.targetClassId(), semesterId).semester();
    }

    public TransferScoreData loadScoreData(TransferScoreDataRequest request) {
        return dataLoader.loadScoreData(request);
    }

    private AcademicYear loadAndValidateYear(StudentYearEnrollment enrollment) {
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw conflict("Chỉ enrollment ACTIVE mới được chuyển lớp");
        }
        AcademicYear year = lookupService.findAcademicYear(enrollment.getAcademicYearId());
        if (year.getStatus() != AcademicYearStatus.ACTIVE) {
            throw conflict("Năm học chưa ACTIVE");
        }
        return year;
    }

    private void validateDifferentClasses(SchoolClass sourceClass, SchoolClass targetClass) {
        if (sourceClass.getId().equals(targetClass.getId())) {
            throw conflict("Lớp đích phải khác lớp hiện tại");
        }
    }

    private void validateSemester(AcademicYear year, Semester semester) {
        if (semester != null && !year.getId().equals(semester.getAcademicYearId())) {
            throw conflict("Học kỳ không thuộc năm học của enrollment");
        }
    }

    private SchoolClass validateTargetClass(Long classId, AcademicYear year) {
        SchoolClass schoolClass = lookupService.findSchoolClass(classId);
        if (!year.getId().equals(schoolClass.getAcademicYearId())) {
            throw conflict("Lớp không thuộc năm học đã chọn");
        }
        if (schoolClass.getStatus() != SchoolClassStatus.ACTIVE) {
            throw conflict("Lớp chưa ACTIVE");
        }
        return schoolClass;
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }

    private AppException badRequest(String message) {
        return new AppException(HttpStatus.BAD_REQUEST, message);
    }
}
