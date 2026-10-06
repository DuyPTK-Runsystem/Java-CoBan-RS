package com.JavaTraining.BaiTap_RS.timetableagent.service;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentModelException;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentModelGateway;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentToolCall;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqExecuteTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.response.ResTimetableAgentActionStateDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentAction;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentProposal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimetableAgentOwnedActionService {

    private final TimetableAgentActionReservationService reservationService;
    private final TimetableAgentActionDispatcher dispatcher;
    private final TimetableAgentCommandExecutor commandExecutor;
    private final TimetableAgentModelActionService modelActionService;

    public ResTimetableAgentActionStateDTO execute(TimetableAgentModelGateway gateway, TimetableAgentProposal proposal,
            ReqExecuteTimetableAgentProposalDTO request, TimetableAgentAction action, Long actorId, Long proposalId) {
        String leaseToken = action.getLeaseToken();
        try {
            TimetableAgentToolCall call = modelActionService.requestSave(gateway, proposalId.toString(),
                    request.proposalVersion(), proposal.getTargetRevisionId(), proposal.getExpectedVersion());
            dispatcher.requireAuthorizedSingleCall(call, proposalId.toString(), request.proposalVersion(),
                    proposal.getTargetRevisionId(), proposal.getExpectedVersion());
            return commandExecutor.save(action.getId(), actorId, leaseToken, proposalId, request.proposalVersion());
        } catch (AppException exception) {
            reservationService.fail(action.getId(), leaseToken, classifyAppException(exception));
            throw exception;
        } catch (TimetableAgentModelException exception) {
            String errorCode = classifyModelException(exception);
            reservationService.fail(action.getId(), leaseToken, errorCode);
            throw new AppException(statusFor(errorCode), messageFor(errorCode), exception);
        }
    }

    private String classifyAppException(AppException exception) {
        return exception.getStatus() == HttpStatus.CONFLICT ? "STALE" : "ACTION_REJECTED";
    }

    private String classifyModelException(TimetableAgentModelException exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && (message.toLowerCase(java.util.Locale.ROOT).contains("timeout")
                    || message.toLowerCase(java.util.Locale.ROOT).contains("time limit"))) {
                return "MODEL_TIMEOUT";
            }
            current = current.getCause();
        }
        return "MODEL_UNAVAILABLE";
    }

    private HttpStatus statusFor(String errorCode) {
        return "MODEL_TIMEOUT".equals(errorCode) ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY;
    }

    private String messageFor(String errorCode) {
        return "MODEL_TIMEOUT".equals(errorCode)
                ? "The timetable save action exceeded its configured time limit."
                : "The timetable save action could not be completed by the configured model.";
    }
}
