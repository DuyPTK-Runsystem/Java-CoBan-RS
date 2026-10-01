package com.JavaTraining.BaiTap_RS.timetableagent.service;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentActionStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentActionStateDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentAction;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposal;
import com.JavaTraining.BaiTap_RS.timetableagent.repository.TimetableAgentActionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TimetableAgentActionStateService {

    private final TimetableAgentActionRepository actionRepository;
    private final TimetableAgentOrchestrator orchestrator;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ResTimetableAgentActionStateDTO get(Long actorId, Long actionId) {
        TimetableAgentAction action = actionRepository.findByIdAndActorId(actionId, actorId)
                .orElseThrow(() -> notFound("Action was not found."));
        return state(action);
    }

    @Transactional(readOnly = true)
    public ResTimetableAgentActionStateDTO getByKey(Long actorId, String key) {
        validateKey(key);
        TimetableAgentAction action = actionRepository.findByActorIdAndIdempotencyKey(actorId, key)
                .orElseThrow(() -> notFound("Action was not found."));
        return state(action);
    }

    public ResTimetableAgentActionStateDTO state(TimetableAgentAction action) {
        TimetableAgentActionStatus status = TimetableAgentActionStatus.valueOf(action.getStatus());
        if (status == TimetableAgentActionStatus.SAVED_DRAFT && action.getReceiptJson() != null) {
            return read(action.getReceiptJson());
        }
        TimetableAgentProposal proposal = findProposal(action);
        return new ResTimetableAgentActionStateDTO(action.getId().toString(), action.getProposalId().toString(),
                proposal == null ? null : proposal.getTargetRevisionId(), status, null, null, action.getCommittedAt(),
                "PENDING".equals(action.getStatus()) ? action.getLeaseExpiresAt() : null,
                action.getErrorCode(), "FAILED".equals(action.getStatus()) && isRetryable(action.getErrorCode()));
    }

    public boolean isRetryable(String errorCode) {
        return "MODEL_UNAVAILABLE".equals(errorCode) || "MODEL_TIMEOUT".equals(errorCode)
                || "PROVIDER_ERROR".equals(errorCode);
    }

    private TimetableAgentProposal findProposal(TimetableAgentAction action) {
        try {
            return orchestrator.requireProposal(action.getActorId(), action.getProposalId());
        } catch (AppException exception) {
            return null;
        }
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > 255) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Idempotency-Key must contain 1 to 255 characters.");
        }
    }

    private ResTimetableAgentActionStateDTO read(String value) {
        try {
            return objectMapper.readValue(value, ResTimetableAgentActionStateDTO.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Action receipt is invalid.", exception);
        }
    }

    private AppException notFound(String message) {
        return new AppException(HttpStatus.NOT_FOUND, message);
    }
}
