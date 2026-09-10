package com.JavaTraining.BaiTap_RS.placement.service.support;

import java.util.List;

import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.response.ResPlacementResultDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.response.ResPlacementSessionDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.response.ResPlacementTargetClassDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementCandidate;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementResult;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlacementResponseMapper {
    private final PlacementScopeService scopeService;

    public ResPlacementSessionDTO session(PlacementSession session, List<PlacementCandidate> candidates,
            List<PlacementResult> results) {
        List<PlacementTarget> targets = scopeService.targets(session);
        long knownGender = candidates.stream().filter(candidate -> validGender(candidate.getGenderSnapshot())).count();
        long male = candidates.stream().filter(candidate -> "MALE".equals(candidate.getGenderSnapshot())).count();
        return new ResPlacementSessionDTO(session.getId(), session.getAcademicYearId(), session.getTargetGradeId(),
                session.getStatus(), session.getRuleVersion(), session.getVersion(),
                targets.stream().map(PlacementTarget::classId).toList(), targets.stream()
                        .map(target -> targetDto(target, knownGender, male)).toList(),
                results.stream().map(this::result).toList());
    }

    public ResPlacementResultDTO result(PlacementResult result) {
        return new ResPlacementResultDTO(result.getId(), result.getStudentId(), result.getTargetClassId(),
                result.getResultStatus(), result.getScore(), result.getIssueCode(), result.getIssueSeverity(),
                result.getExplanation());
    }

    private ResPlacementTargetClassDTO targetDto(PlacementTarget target, long knownGender, long male) {
        int capacity = target.capacity() == null ? 0 : target.capacity();
        int targetMale = knownGender == 0 ? 0 : (int) Math.round(capacity * (double) male / knownGender);
        return new ResPlacementTargetClassDTO(target.classId(), target.classCode(), target.className(),
                target.profile(), target.capacity(), targetMale, Math.max(0, capacity - targetMale));
    }

    private boolean validGender(String gender) {
        return "MALE".equals(gender) || "FEMALE".equals(gender);
    }
}
