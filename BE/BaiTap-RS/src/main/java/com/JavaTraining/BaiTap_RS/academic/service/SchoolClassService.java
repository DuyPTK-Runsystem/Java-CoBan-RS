package com.JavaTraining.BaiTap_RS.academic.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateSchoolClassDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateSchoolClassDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSchoolClassDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.AcademicYear;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.GradeLevel;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClassStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.AcademicYearRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.GradeLevelRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchoolClassService {

    private final AcademicYearRepository academicYearRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolClassValidator validator;

    public SchoolClassService(
            AcademicYearRepository academicYearRepository,
            GradeLevelRepository gradeLevelRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolClassValidator validator) {
        this.academicYearRepository = academicYearRepository;
        this.gradeLevelRepository = gradeLevelRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public List<ResSchoolClassDTO> listSchoolClasses(Long academicYearId) {
        List<SchoolClass> classes = academicYearId == null
                ? schoolClassRepository.findAll(Sort.by(Sort.Direction.ASC, "classCode"))
                : schoolClassRepository.findAllByAcademicYearIdOrderByClassCodeAsc(academicYearId);
        return classes.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ResSchoolClassDTO createSchoolClass(ReqCreateSchoolClassDTO request) {
        AcademicYear year = findAcademicYear(request.academicYearId());
        GradeLevel grade = findGradeLevel(request.gradeLevelId());
        validator.validateClassCreation(year, grade, request.status());
        validator.rejectDuplicateClassCode(request.academicYearId(), request.classCode(), null);
        SchoolClass schoolClass = new SchoolClass(
                request.academicYearId(),
                request.gradeLevelId(),
                request.classCode(),
                request.className(),
                request.capacity(),
                request.status());
        return toResponse(schoolClassRepository.save(schoolClass));
    }

    @Transactional
    public ResSchoolClassDTO updateSchoolClass(Long id, ReqUpdateSchoolClassDTO request) {
        SchoolClass schoolClass = findSchoolClass(id);
        validator.ensureClassNotClosed(schoolClass);
        AcademicYear year = findAcademicYear(schoolClass.getAcademicYearId());
        GradeLevel grade = findGradeLevel(request.gradeLevelId());
        validator.validateClassCreation(year, grade, request.status());
        validator.rejectDuplicateClassCode(schoolClass.getAcademicYearId(), request.classCode(), id);
        validator.rejectGradeChangeWithEnrollment(id, request.gradeLevelId(), schoolClass.getGradeLevelId());
        schoolClass.setGradeLevelId(request.gradeLevelId());
        schoolClass.setClassCode(request.classCode());
        schoolClass.setClassName(request.className());
        schoolClass.setCapacity(request.capacity());
        schoolClass.setStatus(request.status());
        return toResponse(schoolClass);
    }

    // BR-CLASS-006: lớp đóng chỉ được xem và không nhận enrollment mới.
    @Transactional
    public ResSchoolClassDTO closeSchoolClass(Long id) {
        SchoolClass schoolClass = findSchoolClass(id);
        validator.ensureClassNotClosed(schoolClass);
        schoolClass.setStatus(SchoolClassStatus.CLOSED);
        return toResponse(schoolClass);
    }

    // BR-CLASS-011: không xóa lớp đã phát sinh enrollment hoặc lịch sử.
    @Transactional
    public void deleteSchoolClass(Long id) {
        SchoolClass schoolClass = findSchoolClass(id);
        validator.validateDelete(id);
        schoolClassRepository.delete(schoolClass);
    }

    private AcademicYear findAcademicYear(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy năm học"));
    }

    private GradeLevel findGradeLevel(Long id) {
        return gradeLevelRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khối"));
    }

    private SchoolClass findSchoolClass(Long id) {
        return schoolClassRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp"));
    }

    private ResSchoolClassDTO toResponse(SchoolClass schoolClass) {
        return new ResSchoolClassDTO(
                schoolClass.getId(),
                schoolClass.getAcademicYearId(),
                schoolClass.getGradeLevelId(),
                schoolClass.getClassCode(),
                schoolClass.getClassName(),
                schoolClass.getCapacity(),
                schoolClass.getStatus());
    }

}
