package com.JavaTraining.BaiTap_RS.academic.service;

import java.time.LocalDate;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateAcademicYearDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateAcademicYearDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYearStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AcademicYearValidator {

    private final AcademicYearRepository academicYearRepository;

    public AcademicYearValidator(AcademicYearRepository academicYearRepository) {
        this.academicYearRepository = academicYearRepository;
    }

    public void validateCreate(ReqCreateAcademicYearDTO request) {
        validateDates(request.startDate(), request.endDate());
        rejectDuplicateCode(request.code(), null);
        validateActiveYear(request.status(), null);
    }

    public void validateUpdate(Long id, ReqUpdateAcademicYearDTO request) {
        validateDates(request.startDate(), request.endDate());
        rejectDuplicateCode(request.code(), id);
        validateActiveYear(request.status(), id);
    }

    public void ensureOpen(AcademicYear academicYear) {
        if (academicYear.getStatus() == AcademicYearStatus.CLOSED) {
            throw conflict("Năm học đã đóng");
        }
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (!endDate.isAfter(startDate)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Ngày kết thúc phải sau ngày bắt đầu");
        }
    }

    private void rejectDuplicateCode(String code, Long id) {
        boolean duplicate = id == null
                ? academicYearRepository.existsByCode(code)
                : academicYearRepository.existsByCodeAndIdNot(code, id);
        if (duplicate) {
            throw conflict("Mã năm học đã tồn tại");
        }
    }

    private void validateActiveYear(AcademicYearStatus status, Long id) {
        if (status != AcademicYearStatus.ACTIVE) {
            return;
        }
        boolean activeExists = id == null
                ? academicYearRepository.existsByStatus(AcademicYearStatus.ACTIVE)
                : academicYearRepository.existsByStatusAndIdNot(AcademicYearStatus.ACTIVE, id);
        if (activeExists) {
            throw conflict("Chỉ được có một năm học ACTIVE");
        }
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}
