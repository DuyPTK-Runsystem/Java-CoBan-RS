package com.JavaTraining.BaiTap_RS.placement.service.support;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.academic.repository.SchoolClassRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.domain.entity.EnrollmentStatus;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public final class PlacementScopeValidator {
    private final SchoolClassRepository classes;
    private final StudentYearEnrollmentRepository enrollments;

    public PlacementScopeValidator(SchoolClassRepository classes, StudentYearEnrollmentRepository enrollments) {
        this.classes = classes;
        this.enrollments = enrollments;
    }

    public List<SchoolClass> targetClasses(Long academicYearId, List<Long> classIds) {
        List<SchoolClass> result = classes.findAllByIdInAndAcademicYearIdOrderByClassCodeAsc(classIds, academicYearId);
        if (result.size() != classIds.stream().distinct().count()) {
            throw new AppException(HttpStatus.CONFLICT, "Lớp đích không thuộc năm học");
        }
        return result;
    }

    public void validateCapacitySnapshot(List<PlacementTarget> targets, List<SchoolClass> currentClasses) {
        Map<Long, SchoolClass> classesById = currentClasses.stream()
                .collect(java.util.stream.Collectors.toMap(SchoolClass::getId, value -> value));
        for (PlacementTarget target : targets) {
            if (!Objects.equals(target.capacity(), classesById.get(target.classId()).getCapacity())) {
                throw new AppException(HttpStatus.CONFLICT,
                        "Sức chứa lớp đích đã thay đổi; cần tạo lại phiên");
            }
        }
    }

    public void validateTargetAtConfirm(PlacementSession session, Map<Long, Long> assignedCounts,
            SchoolClass currentClass, PlacementTarget target) {
        if (!Objects.equals(currentClass.getGradeLevelId(), session.getTargetGradeId())) {
            throw new AppException(HttpStatus.CONFLICT, "Khối của lớp đích đã thay đổi");
        }
        if (assignedCounts.getOrDefault(target.classId(), 0L) > availableCapacity(currentClass)) {
            throw new AppException(HttpStatus.CONFLICT, "Sức chứa lớp đích đã thay đổi");
        }
    }

    public int availableCapacity(SchoolClass schoolClass) {
        int capacity = schoolClass.getCapacity() == null ? Integer.MAX_VALUE : schoolClass.getCapacity();
        long occupied = enrollments.countByCurrentClassIdAndStatus(schoolClass.getId(), EnrollmentStatus.ACTIVE);
        return capacity == Integer.MAX_VALUE ? capacity : Math.max(0, capacity - Math.toIntExact(occupied));
    }
}
