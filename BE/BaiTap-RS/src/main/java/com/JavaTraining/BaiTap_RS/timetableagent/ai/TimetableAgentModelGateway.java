package com.JavaTraining.BaiTap_RS.timetableagent.ai;

import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentModelProposalDTO;

/** Provider-neutral boundary. Implementations must not execute returned tool calls. */
public interface TimetableAgentModelGateway {

    default boolean supportsNativeStructuredOutput() {
        return false;
    }

    default boolean supportsSaveToolCalling() {
        return false;
    }

    TimetableAgentModelProposalDTO propose(TimetableAgentProposalPrompt prompt);

    TimetableAgentToolCall requestSave(TimetableAgentActionPrompt prompt);
}
