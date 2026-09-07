package com.JavaTraining.BaiTap_RS.academic.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateAcademicYearDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateAcademicYearDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResAcademicYearDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("PMD.GuardLogStatement")
public class AcademicYearService {

    private final AcademicYearRepository academicYearRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentYearEnrollmentRepository enrollmentRepository;
    private final AcademicYearValidator validator;

    public AcademicYearService(
            AcademicYearRepository academicYearRepository,
            SchoolClassRepository schoolClassRepository,
            StudentYearEnrollmentRepository enrollmentRepository,
            AcademicYearValidator validator) {
        this.academicYearRepository = academicYearRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public List<ResAcademicYearDTO> listAcademicYears() {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                AcademicYearService.class,
                "AcademicYearService.listAcademicYears");
        return academicYearRepository.findAll(Sort.by(Sort.Direction.DESC, "startDate"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ResAcademicYearDTO createAcademicYear(ReqCreateAcademicYearDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                AcademicYearService.class,
                "AcademicYearService.createAcademicYear");
        validator.validateCreate(request);
        AcademicYear academicYear = new AcademicYear(
                request.code(),
                request.startDate(),
                request.endDate(),
                request.status(),
                request.notes());
        return toResponse(academicYearRepository.save(academicYear));
    }

    @Transactional
    public ResAcademicYearDTO updateAcademicYear(Long id, ReqUpdateAcademicYearDTO request) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                AcademicYearService.class,
                "AcademicYearService.updateAcademicYear");
        AcademicYear academicYear = findAcademicYear(id);
        validator.ensureOpen(academicYear);
        validator.validateUpdate(id, request);
        academicYear.setCode(request.code());
        academicYear.setStartDate(request.startDate());
        academicYear.setEndDate(request.endDate());
        academicYear.setStatus(request.status());
        academicYear.setNotes(request.notes());
        return toResponse(academicYear);
    }

    // BR-AY-005: đóng năm học không xóa dữ liệu lịch sử.
    @Transactional
    public ResAcademicYearDTO closeAcademicYear(Long id) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                AcademicYearService.class,
                "AcademicYearService.closeAcademicYear");
        AcademicYear academicYear = findAcademicYear(id);
        validator.ensureOpen(academicYear);
        academicYear.setStatus(AcademicYearStatus.CLOSED);
        return toResponse(academicYear);
    }

    // BR-AY-004: không xóa năm học đã có lớp hoặc phân lớp.
    @Transactional
    public void deleteAcademicYear(Long id) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                AcademicYearService.class,
                "AcademicYearService.deleteAcademicYear");
        AcademicYear academicYear = findAcademicYear(id);
        if (schoolClassRepository.existsByAcademicYearId(id)
                || enrollmentRepository.existsByAcademicYearId(id)) {
            throw conflict("Không thể xóa năm học đã phát sinh dữ liệu");
        }
        academicYearRepository.delete(academicYear);
    }

    private AcademicYear findAcademicYear(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy năm học"));
    }

    private ResAcademicYearDTO toResponse(AcademicYear year) {
        return new ResAcademicYearDTO(
                year.getId(),
                year.getCode(),
                year.getStartDate(),
                year.getEndDate(),
                year.getStatus(),
                year.getNotes());
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}
