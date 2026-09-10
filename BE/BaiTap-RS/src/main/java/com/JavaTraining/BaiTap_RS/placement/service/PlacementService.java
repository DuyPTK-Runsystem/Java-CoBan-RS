package com.JavaTraining.BaiTap_RS.placement.service;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.common.audit.AuditContext;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.requests.ReqConfirmPlacementDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.requests.ReqCreatePlacementSessionDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.requests.ReqPlacementActionDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.requests.ReqUpdatePlacementSessionDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.response.ResPlacementResultDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.response.ResPlacementSessionDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementResult;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSession;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSessionStatus;
import com.JavaTraining.BaiTap_RS.placement.repository.PlacementSessionRepository;
import com.JavaTraining.BaiTap_RS.placement.service.support.PlacementCandidateSnapshotService;
import com.JavaTraining.BaiTap_RS.placement.service.support.PlacementConfirmationSupport;
import com.JavaTraining.BaiTap_RS.placement.service.support.PlacementConfirmationValidator;
import com.JavaTraining.BaiTap_RS.placement.service.support.PlacementScopeService;
import com.JavaTraining.BaiTap_RS.placement.service.support.PlacementSessionAccess;
import com.JavaTraining.BaiTap_RS.placement.service.support.PlacementSessionRules;
import com.JavaTraining.BaiTap_RS.placement.service.support.PlacementSimulationSupport;
import com.JavaTraining.BaiTap_RS.placement.service.support.PlacementTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlacementService {
    private final PlacementSessionRepository sessions;
    private final PlacementScopeService scopeService;
    private final PlacementCandidateSnapshotService candidateSnapshots;
    private final PlacementConfirmationValidator confirmationValidator;
    private final PlacementSessionAccess access;
    private final PlacementSessionRules rules;
    private final PlacementConfirmationSupport confirmation;
    private final PlacementSimulationSupport simulation;

    @Transactional
    public ResPlacementSessionDTO create(ReqCreatePlacementSessionDTO request) {
        rules.validateCandidates(request);
        List<PlacementTarget> targets = scopeService.createScope(request.academicYearId(), request.targetGradeId(),
                rules.createProfiles(request.targetClasses()));
        PlacementSession session = sessions.save(new PlacementSession(request.academicYearId(), request.targetGradeId(),
                request.ruleVersion(), scopeService.snapshot(targets), AuditContext.currentUserId()));
        candidateSnapshots.snapshot(session, request.candidates());
        confirmation.audit("PLACEMENT_SESSION_CREATED", session.getId(), "classes=" + targets.size());
        return access.response(session, List.of());
    }

    @Transactional(readOnly = true)
    public ResPlacementSessionDTO get(Long id) {
        return access.response(access.find(id), List.of());
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO<ResPlacementResultDTO> getResults(Long id, Pageable pageable) {
        access.find(id);
        return access.pageResults(id, pageable);
    }

    @Transactional
    public ResPlacementSessionDTO update(Long id, ReqUpdatePlacementSessionDTO request) {
        PlacementSession session = access.find(id);
        rules.checkVersion(session, request.expectedVersion());
        rules.require(session, PlacementSessionStatus.DRAFT);
        session.setScopeSnapshot(scopeService.snapshot(scopeService.updateScope(session,
                rules.updateProfiles(request.targetClasses()))));
        sessions.save(session);
        return access.response(session, access.allResults(id));
    }

    @Transactional
    public ResPlacementSessionDTO simulate(Long id, ReqPlacementActionDTO request) {
        PlacementSession session = access.findLocked(id);
        rules.checkVersion(session, request.expectedVersion());
        rules.require(session, PlacementSessionStatus.DRAFT, PlacementSessionStatus.SIMULATED,
                PlacementSessionStatus.READY_FOR_CONFIRM);
        return simulation.simulate(session);
    }

    @Transactional
    public ResPlacementSessionDTO confirm(Long id, ReqConfirmPlacementDTO request) {
        rules.requireIdempotencyKey(request.idempotencyKey());
        PlacementSession session = access.findLocked(id);
        Optional<ResPlacementSessionDTO> replay = confirmation.replayConfirmed(id, request.idempotencyKey());
        if (replay.isPresent()) {
            return replay.get();
        }
        rules.checkVersion(session, request.expectedVersion());
        rules.require(session, PlacementSessionStatus.READY_FOR_CONFIRM);
        List<PlacementResult> resultValues = access.allResults(id);
        rules.requireNoBlockingIssue(resultValues);
        confirmationValidator.validate(session, resultValues);
        confirmation.createAutomaticEnrollments(session, resultValues);
        session.setStatus(PlacementSessionStatus.CONFIRMED);
        session.setConfirmIdempotencyKey(request.idempotencyKey());
        sessions.save(session);
        confirmation.audit("PLACEMENT_SESSION_CONFIRMED", id, "results=" + resultValues.size());
        return access.response(session, resultValues);
    }

    @Transactional
    public ResPlacementSessionDTO cancel(Long id, ReqPlacementActionDTO request) {
        PlacementSession session = access.findLocked(id);
        rules.checkVersion(session, request.expectedVersion());
        rules.require(session, PlacementSessionStatus.DRAFT, PlacementSessionStatus.SIMULATED,
                PlacementSessionStatus.READY_FOR_CONFIRM);
        session.setStatus(PlacementSessionStatus.CANCELLED);
        sessions.save(session);
        confirmation.audit("PLACEMENT_SESSION_CANCELLED", id, null);
        return access.response(session, access.allResults(id));
    }
}
