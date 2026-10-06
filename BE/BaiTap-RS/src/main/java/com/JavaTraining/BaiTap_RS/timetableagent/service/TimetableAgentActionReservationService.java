package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.JavaTraining.BaiTap_RS.timetableagent.config.TimetableAgentProperties;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentAction;
import com.JavaTraining.BaiTap_RS.timetableagent.repository.TimetableAgentActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TimetableAgentActionReservationService {

    private final TimetableAgentActionRepository actionRepository;
    private final TimetableAgentProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Reservation reserve(Long actorId, String key, String requestHash, Long proposalId,
            Long targetRevisionId) {
        Instant now = Instant.now(Clock.systemUTC());
        TimetableAgentAction action = actionRepository.findByActorIdAndIdempotencyKeyForUpdate(actorId, key)
                .orElse(null);
        if (action == null) {
            action = new TimetableAgentAction(proposalId, actorId, key, requestHash, null);
            action.setLeaseExpiresAt(now.plus(properties.getProviderTimeout()).plusSeconds(10));
            return new Reservation(actionRepository.saveAndFlush(action), true);
        }
        if (!action.getRequestHash().equals(requestHash)) {
            throw new IllegalStateException("IDEMPOTENCY_KEY_REUSED");
        }
        if ("SAVED_DRAFT".equals(action.getStatus())) {
            return new Reservation(action, false);
        }
        if ("PENDING".equals(action.getStatus()) && action.getLeaseExpiresAt().isAfter(now)) {
            return new Reservation(action, false);
        }
        action.setStatus("PENDING");
        action.setErrorCode(null);
        action.setLeaseToken(UUID.randomUUID().toString());
        action.setLeaseExpiresAt(now.plus(properties.getProviderTimeout()).plusSeconds(10));
        return new Reservation(actionRepository.saveAndFlush(action), true);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Long actionId, String leaseToken, String errorCode) {
        actionRepository.findByIdForUpdate(actionId).ifPresent(action -> {
            if ("PENDING".equals(action.getStatus()) && leaseToken.equals(action.getLeaseToken())) {
                action.setStatus("FAILED");
                action.setErrorCode(errorCode);
            }
        });
    }

    public record Reservation(TimetableAgentAction action, boolean owner) {
    }
}
