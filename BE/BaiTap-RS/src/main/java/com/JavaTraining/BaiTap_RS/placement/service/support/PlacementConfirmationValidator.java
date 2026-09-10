package com.JavaTraining.BaiTap_RS.placement.service.support;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.enrollment.repository.StudentYearEnrollmentRepository;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementCandidate;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementResult;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementResultStatus;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSession;
import com.JavaTraining.BaiTap_RS.placement.repository.PlacementCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlacementConfirmationValidator {
    private final PlacementScopeService scopeService;
    private final PlacementCandidateRepository candidates;
    private final StudentYearEnrollmentRepository enrollments;

    public void validate(PlacementSession session, List<PlacementResult> results) {
        Map<Long, Long> assignedCounts = results.stream().filter(this::isAutomatic)
                .collect(Collectors.groupingBy(PlacementResult::getTargetClassId, Collectors.counting()));
        scopeService.validateConfirmScope(session, assignedCounts);
        Map<Long, PlacementCandidate> candidatesByStudent = candidates
                .findAllBySessionIdOrderByStudentIdAsc(session.getId())
                .stream().collect(Collectors.toMap(PlacementCandidate::getStudentId, candidate -> candidate));
        results.stream().filter(this::isAutomatic)
                .forEach(result -> validateCandidate(session, candidatesByStudent.get(result.getStudentId()), result));
    }

    private boolean isAutomatic(PlacementResult result) {
        return result.getResultStatus() == PlacementResultStatus.AUTO_ASSIGNED;
    }

    private void validateCandidate(PlacementSession session, PlacementCandidate candidate, PlacementResult result) {
        if (candidate == null || !Objects.equals(candidate.getTargetGradeId(), session.getTargetGradeId())
                || candidate.getScore() == null || !validGender(candidate.getGenderSnapshot())) {
            throw new AppException(HttpStatus.CONFLICT, "Dữ liệu ứng viên đã thay đổi");
        }
        if (enrollments.existsByStudentIdAndAcademicYearId(result.getStudentId(), session.getAcademicYearId())) {
            throw new AppException(HttpStatus.CONFLICT, "Học sinh đã có enrollment trong năm học đích");
        }
    }

    private boolean validGender(String gender) {
        return "MALE".equals(gender) || "FEMALE".equals(gender);
    }
}
