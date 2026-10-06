package com.JavaTraining.BaiTap_RS.timetableagent.service;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetableagent.config.TimetableAgentProperties;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentModelProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqCreateTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimetableAgentInputLimits {

    private final TimetableAgentProperties properties;

    public void requireConfigured() {
        if (!properties.hasRequiredBounds()) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Timetable agent size limits must be configured before enabling the feature.");
        }
    }

    public void validateRequest(ReqCreateTimetableAgentProposalDTO request) {
        requireConfigured();
        int requestCharacters = length(request.userRequest()) + length(request.preferences());
        int demandCount = request.demands().size();
        int lockedCount = request.lockedEntryIds() == null ? 0 : request.lockedEntryIds().size();
        if (request.classIds().size() > properties.getMaxClassCount()
                || demandCount > properties.getMaxProposalEntries()
                || lockedCount > properties.getMaxProposalEntries()
                || requestCharacters > properties.getMaxRequestCharacters()
                || length(request.preferences()) > properties.getMaxPreferencesCharacters()) {
            throw new AppException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Timetable agent request exceeds configured input limits.");
        }
    }

    public void validateSnapshot(TimetableAgentSnapshot snapshot) {
        if (snapshot.snapshotJson().length() > properties.getMaxContextCharacters()) {
            throw new AppException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Timetable agent context exceeds its configured size limit.");
        }
    }

    public void validateProposal(TimetableAgentModelProposalDTO proposal) {
        if (proposal.entries().size() > properties.getMaxProposalEntries()) {
            throw new AppException(HttpStatus.BAD_GATEWAY,
                    "The timetable model returned more entries than the configured limit.");
        }
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }
}
