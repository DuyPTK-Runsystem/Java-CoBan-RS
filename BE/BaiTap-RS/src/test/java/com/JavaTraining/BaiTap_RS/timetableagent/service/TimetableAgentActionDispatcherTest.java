package com.JavaTraining.BaiTap_RS.timetableagent.service;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetableagent.ai.TimetableAgentToolCall;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TimetableAgentActionDispatcherTest {

    private static final String PROPOSAL_ID = "p-1";
    private static final String SAVE_TOOL = "saveTimetableDraft";
    private static final String REVISION_BINDING = "\"targetRevisionId\":77,\"expectedVersion\":9}";
    private final TimetableAgentActionDispatcher dispatcher = new TimetableAgentActionDispatcher();

    @Test
    void requireAuthorizedSingleCallAcceptsExactApprovedBinding() {
        TimetableAgentToolCall call = call("{\"proposalId\":\"p-1\",\"proposalVersion\":3,"
                + REVISION_BINDING);

        dispatcher.requireAuthorizedSingleCall(call, PROPOSAL_ID, 3L, 77L, 9L);
    }

    @Test
    void requireAuthorizedSingleCallRejectsWrongToolAndMissingCallIdentity() {
        java.util.Arrays.asList(new TimetableAgentToolCall("call-1", "otherTool", "{}"),
                new TimetableAgentToolCall(" ", SAVE_TOOL, "{}"), null)
                .forEach(this::assertRejected);
    }

    @Test
    void requireAuthorizedSingleCallRejectsDuplicateKeysTrailingTokensAndUnknownFields() {
        java.util.List.of(
                call("{\"proposalId\":\"p-1\",\"proposalId\":\"p-1\","
                        + "\"proposalVersion\":3," + REVISION_BINDING),
                call("{\"proposalId\":\"p-1\",\"proposalVersion\":3," + REVISION_BINDING + " {}"),
                call("{\"proposalId\":\"p-1\",\"proposalVersion\":3,"
                        + REVISION_BINDING.replace("}", ",\"execute\":true}")))
                .forEach(this::assertRejected);
    }

    @Test
    void requireAuthorizedSingleCallRejectsCoercionFractionalAndMismatchedBindings() {
        java.util.List.of(
                call("{\"proposalId\":\"p-1\",\"proposalVersion\":\"3\"," 
                        + "\"targetRevisionId\":77,\"expectedVersion\":9}"),
                call("{\"proposalId\":\"p-1\",\"proposalVersion\":3.0," 
                        + "\"targetRevisionId\":77,\"expectedVersion\":9}"),
                call("{\"proposalId\":\"p-2\",\"proposalVersion\":3," 
                        + "\"targetRevisionId\":77,\"expectedVersion\":9}"))
                .forEach(this::assertRejected);
    }

    private TimetableAgentToolCall call(String arguments) {
        return new TimetableAgentToolCall("call-1", SAVE_TOOL, arguments);
    }

    private void assertRejected(TimetableAgentToolCall call) {
        AppException exception = Assertions.assertThrows(AppException.class,
                () -> dispatcher.requireAuthorizedSingleCall(call, PROPOSAL_ID, 3L, 77L, 9L),
                "unauthorized or malformed tool calls must fail closed");
        Assertions.assertEquals(422, exception.getStatus().value(), "invalid save tools must map to 422");
    }
}

