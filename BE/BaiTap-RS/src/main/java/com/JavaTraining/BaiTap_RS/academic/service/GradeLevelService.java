package com.JavaTraining.BaiTap_RS.academic.service;

import java.util.List;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqCreateGradeLevelDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateGradeLevelDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResGradeLevelDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.GradeLevel;
import com.JavaTraining.BaiTap_RS.academic.repository.GradeLevelRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradeLevelService {

    private final GradeLevelRepository gradeLevelRepository;
    private final GradeLevelValidator validator;

    public GradeLevelService(
            GradeLevelRepository gradeLevelRepository,
            GradeLevelValidator validator) {
        this.gradeLevelRepository = gradeLevelRepository;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public List<ResGradeLevelDTO> listGradeLevels() {
        return gradeLevelRepository.findAll(Sort.by(Sort.Direction.ASC, "displayOrder"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ResGradeLevelDTO createGradeLevel(ReqCreateGradeLevelDTO request) {
        validator.validateCreate(request);
        GradeLevel grade = new GradeLevel(
                request.code(),
                request.name(),
                request.gradeLevel(),
                request.displayOrder(),
                request.nextGradeId(),
                request.active(),
                request.description());
        return toResponse(gradeLevelRepository.save(grade));
    }

    @Transactional
    public ResGradeLevelDTO updateGradeLevel(Long id, ReqUpdateGradeLevelDTO request) {
        GradeLevel grade = findGradeLevel(id);
        validator.validateUpdate(id, grade, request);
        grade.setCode(request.code());
        grade.setName(request.name());
        grade.setLevel(request.gradeLevel());
        grade.setDisplayOrder(request.displayOrder());
        grade.setNextGradeId(request.nextGradeId());
        grade.setActive(request.active());
        grade.setDescription(request.description());
        return toResponse(grade);
    }

    // BR-GRADE-005: khối đã được tham chiếu không bị xóa vật lý.
    @Transactional
    public void deleteGradeLevel(Long id) {
        GradeLevel grade = findGradeLevel(id);
        validator.validateDelete(id);
        gradeLevelRepository.delete(grade);
    }

    private GradeLevel findGradeLevel(Long id) {
        return gradeLevelRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khối"));
    }

    private ResGradeLevelDTO toResponse(GradeLevel grade) {
        return new ResGradeLevelDTO(
                grade.getId(),
                grade.getCode(),
                grade.getName(),
                grade.getLevel(),
                grade.getDisplayOrder(),
                grade.getNextGradeId(),
                grade.isActive(),
                grade.getDescription());
    }

}
