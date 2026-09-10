package com.JavaTraining.BaiTap_RS.placement.service.support;

import java.util.List;

import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.response.ResPlacementSessionDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementCandidate;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementResult;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSession;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSessionStatus;
import com.JavaTraining.BaiTap_RS.placement.repository.PlacementCandidateRepository;
import com.JavaTraining.BaiTap_RS.placement.repository.PlacementResultRepository;
import com.JavaTraining.BaiTap_RS.placement.repository.PlacementSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public final class PlacementSimulationSupport {
    private final PlacementSessionRepository sessions;
    private final PlacementCandidateRepository candidates;
    private final PlacementResultRepository results;
    private final PlacementScopeService scopeService;
    private final PlacementAllocationEngine allocationEngine;
    private final PlacementSessionAccess access;
    private final PlacementSessionRules rules;

    public ResPlacementSessionDTO simulate(PlacementSession session) {
        results.deleteAllBySessionId(session.getId());
        List<PlacementTarget> targets = scopeService.targets(session);
        List<PlacementCandidate> candidateValues = candidates
                .findAllBySessionIdOrderByStudentIdAsc(session.getId());
        List<PlacementResult> allocated = allocationEngine.allocate(session.getId(), candidateValues, targets,
                scopeService.availableCapacity(session.getAcademicYearId(), targets));
        results.saveAll(allocated);
        session.setStatus(rules.hasBlockingIssue(allocated) ? PlacementSessionStatus.SIMULATED
                : PlacementSessionStatus.READY_FOR_CONFIRM);
        sessions.save(session);
        return access.response(session, allocated, candidateValues);
    }
}
