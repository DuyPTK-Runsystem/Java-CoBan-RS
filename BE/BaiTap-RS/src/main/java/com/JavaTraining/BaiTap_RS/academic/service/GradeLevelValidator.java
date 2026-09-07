package com.JavaTraining.BaiTap_RS.academic.service;

import java.util.HashSet;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateGradeLevelDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateGradeLevelDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.GradeLevel;
import com.JavaTraining.BaiTap_RS.academic.repository.GradeLevelRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class GradeLevelValidator {

    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolClassRepository schoolClassRepository;

    public GradeLevelValidator(
            GradeLevelRepository gradeLevelRepository,
            SchoolClassRepository schoolClassRepository) {
        this.gradeLevelRepository = gradeLevelRepository;
        this.schoolClassRepository = schoolClassRepository;
    }

    public void validateCreate(ReqCreateGradeLevelDTO request) {
        validateUniqueness(request.code(), request.gradeLevel(), null);
        validateNextGrade(request.nextGradeId(), null);
    }

    public void validateUpdate(Long id, GradeLevel grade, ReqUpdateGradeLevelDTO request) {
        validateUniqueness(request.code(), request.gradeLevel(), id);
        validateNextGrade(request.nextGradeId(), id);
        if (!request.gradeLevel().equals(grade.getLevel()) && schoolClassRepository.existsByGradeLevelId(id)) {
            throw conflict("Không thể đổi cấp khối đã được lớp tham chiếu");
        }
    }

    public void validateDelete(Long id) {
        if (schoolClassRepository.existsByGradeLevelId(id)
                || gradeLevelRepository.existsByNextGradeId(id)) {
            throw conflict("Không thể xóa khối đã được tham chiếu");
        }
    }

    private void validateUniqueness(String code, Integer level, Long id) {
        boolean duplicateCode = id == null
                ? gradeLevelRepository.existsByCode(code)
                : gradeLevelRepository.existsByCodeAndIdNot(code, id);
        boolean duplicateLevel = id == null
                ? gradeLevelRepository.existsByLevel(level)
                : gradeLevelRepository.existsByLevelAndIdNot(level, id);
        if (duplicateCode || duplicateLevel) {
            throw conflict("Mã khối hoặc cấp khối đã tồn tại");
        }
    }

    private void validateNextGrade(Long nextGradeId, Long currentId) {
        Long cursor = nextGradeId;
        Set<Long> visited = new HashSet<>();
        while (cursor != null) {
            if (!visited.add(cursor) || currentId != null && currentId.equals(cursor)) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Khối tiếp theo không được tạo chu trình");
            }
            cursor = requireGrade(cursor).getNextGradeId();
        }
    }

    private GradeLevel requireGrade(Long gradeId) {
        return gradeLevelRepository.findById(gradeId).orElseThrow(this::gradeNotFound);
    }

    private AppException gradeNotFound() {
        return new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khối");
    }

    private AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, message);
    }
}
