package com.JavaTraining.BaiTap_RS.placement.service.support;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SchoolClass;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementClassProfile;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlacementScopeService {
    private final PlacementScopeValidator validator;
    private final ObjectMapper mapper;

    public List<PlacementTarget> createScope(Long academicYearId, Long gradeId,
            Map<Long, PlacementClassProfile> profiles) {
        List<Long> classIds = List.copyOf(profiles.keySet());
        List<SchoolClass> targetClasses = validator.targetClasses(academicYearId, classIds);
        if (targetClasses.stream().anyMatch(value -> !Objects.equals(value.getGradeLevelId(), gradeId))) {
            throw new AppException(HttpStatus.CONFLICT, "Lớp đích không thuộc khối xếp lớp");
        }
        return canonicalTargets(targetClasses, profiles);
    }

    public List<PlacementTarget> updateScope(PlacementSession session,
            Map<Long, PlacementClassProfile> profiles) {
        return createScope(session.getAcademicYearId(), session.getTargetGradeId(), profiles);
    }

    public List<PlacementTarget> targets(PlacementSession session) {
        try {
            return mapper.readValue(session.getScopeSnapshot(), new TypeReference<>() { });
        } catch (JsonProcessingException exception) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Snapshot phiên không hợp lệ", exception);
        }
    }

    public String snapshot(List<PlacementTarget> targets) {
        try {
            return mapper.writeValueAsString(targets);
        } catch (JsonProcessingException exception) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu snapshot", exception);
        }
    }

    public Map<Long, Integer> availableCapacity(Long academicYearId, List<PlacementTarget> targets) {
        List<SchoolClass> currentClasses = validator.targetClasses(academicYearId,
                targets.stream().map(PlacementTarget::classId).toList());
        validator.validateCapacitySnapshot(targets, currentClasses);
        return currentClasses.stream().collect(Collectors.toMap(SchoolClass::getId, validator::availableCapacity));
    }

    public void validateConfirmScope(PlacementSession session, Map<Long, Long> assignedCounts) {
        List<PlacementTarget> targets = targets(session);
        List<SchoolClass> currentClasses = validator.targetClasses(session.getAcademicYearId(),
                targets.stream().map(PlacementTarget::classId).toList());
        validator.validateCapacitySnapshot(targets, currentClasses);
        Map<Long, SchoolClass> currentById = currentClasses.stream()
                .collect(Collectors.toMap(SchoolClass::getId, Function.identity()));
        targets.forEach(target -> validator.validateTargetAtConfirm(session, assignedCounts,
                currentById.get(target.classId()), target));
    }

    private List<PlacementTarget> canonicalTargets(List<SchoolClass> targetClasses,
            Map<Long, PlacementClassProfile> profiles) {
        return targetClasses.stream().map(value -> new PlacementTarget(value.getId(), value.getClassCode(),
                value.getClassName(), profiles.get(value.getId()), value.getCapacity())).toList();
    }

}
