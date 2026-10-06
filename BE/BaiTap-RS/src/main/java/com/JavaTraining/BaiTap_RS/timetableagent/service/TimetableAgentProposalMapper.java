package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.Instant;

import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentModelGateway;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentModelGatewayResolver;
import com.JavaTraining.BaiTap_RS.timetableagent.config.TimetableAgentProperties;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentCapabilitiesDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentValidationResult;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposal;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TimetableAgentProposalMapper {

    private final TimetableAgentProperties properties;
    private final TimetableAgentModelGatewayResolver gatewayResolver;

    public ResTimetableAgentProposalDTO response(TimetableAgentProposal stored, TimetableAgentValidationResult result) {
        TimetableAgentModelGateway gateway = gatewayResolver.getIfUnambiguous();
        boolean active = properties.isEnabled() && properties.hasRequiredBounds()
                && gateway != null && gateway.supportsNativeStructuredOutput()
                && Instant.now().isBefore(stored.getExpiresAt());
        boolean reviewable = result.readyForReview() && active
                && stored.getStatus()
                == com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalStatus
                        .READY_FOR_REVIEW;
        boolean executable = active && gateway.supportsSaveToolCalling()
                && stored.getStatus()
                == com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposalStatus.APPROVED;
        return new ResTimetableAgentProposalDTO(stored.getId().toString(), stored.getProposalVersion(),
                stored.getProposalHash(), stored.getTargetRevisionId(), stored.getExpectedVersion(),
                status(stored), stored.getExpiresAt(), stored.getSnapshotId(), result.entries(), result.issues(),
                result.explanation(), result.diff(),
                new ResTimetableAgentCapabilitiesDTO(active, reviewable, executable));
    }

    private TimetableAgentProposalStatus status(TimetableAgentProposal stored) {
        return Instant.now().isAfter(stored.getExpiresAt()) ? TimetableAgentProposalStatus.EXPIRED
                : TimetableAgentProposalStatus.valueOf(stored.getStatus().name());
    }
}
