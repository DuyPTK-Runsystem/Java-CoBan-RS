package com.JavaTraining.BaiTap_RS.placement.service.support;

import java.util.List;

import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.response.ResPlacementResultDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.response.ResPlacementSessionDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementCandidate;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementResult;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSession;
import com.JavaTraining.BaiTap_RS.placement.repository.PlacementCandidateRepository;
import com.JavaTraining.BaiTap_RS.placement.repository.PlacementResultRepository;
import com.JavaTraining.BaiTap_RS.placement.repository.PlacementSessionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public final class PlacementSessionAccess {
    private final PlacementSessionRepository sessions;
    private final PlacementCandidateRepository candidates;
    private final PlacementResultRepository results;
    private final PlacementResponseMapper responseMapper;

    public PlacementSessionAccess(PlacementSessionRepository sessions, PlacementCandidateRepository candidates,
            PlacementResultRepository results, PlacementResponseMapper responseMapper) {
        this.sessions = sessions;
        this.candidates = candidates;
        this.results = results;
        this.responseMapper = responseMapper;
    }

    public PlacementSession find(Long id) {
        return sessions.findById(id).orElseThrow(PlacementSessionAccess::missing);
    }

    public PlacementSession findLocked(Long id) {
        return sessions.findByIdForUpdate(id).orElseThrow(PlacementSessionAccess::missing);
    }

    public List<PlacementResult> allResults(Long id) {
        return results.findAllBySessionIdOrderByStudentIdAsc(id);
    }

    public ResPlacementSessionDTO response(PlacementSession session, List<PlacementResult> resultValues) {
        return response(session, resultValues, candidates.findAllBySessionIdOrderByStudentIdAsc(session.getId()));
    }

    public ResPlacementSessionDTO response(PlacementSession session, List<PlacementResult> resultValues,
            List<PlacementCandidate> candidateValues) {
        return responseMapper.session(session, candidateValues, resultValues);
    }

    public ResultPaginationDTO<ResPlacementResultDTO> pageResults(Long id, Pageable pageable) {
        Page<PlacementResult> page = results.findBySessionIdOrderByStudentIdAsc(id, pageable);
        return new ResultPaginationDTO<>(new ResultPaginationDTO.Meta(page.getNumber(), page.getSize(),
                page.getTotalPages(), page.getTotalElements()),
                page.getContent().stream().map(responseMapper::result).toList());
    }

    private static AppException missing() {
        return new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy phiên xếp lớp");
    }
}
