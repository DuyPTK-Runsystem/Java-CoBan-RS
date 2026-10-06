package com.JavaTraining.BaiTap_RS.timetableagent.ai;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentModelProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentModelProposalStatus;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentProposalEntryDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.StructuredOutputChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

class SpringAiTimetableModelGatewayTest {

    private static final String VALID_PROPOSAL = """
            {
              "schemaVersion":"1",
              "status":"PROPOSED",
              "snapshotId":"snapshot-1",
              "entries":[{
                "assignmentId":11,
                "periodId":21,
                "functionalRoomId":null,
                "validFrom":"2026-10-05",
                "validTo":"2026-12-31"
              }],
              "unresolvedConstraints":[],
              "explanation":"Đề xuất hợp lệ"
            }
            """;

    @Test
    void proposeDecodesStrictProposalAndNullableRoom() {
        SpringAiTimetableModelGateway gateway = gatewayReturning(VALID_PROPOSAL);

        TimetableAgentModelProposalDTO result = gateway.propose(prompt());

        TimetableAgentModelProposalDTO expected = new TimetableAgentModelProposalDTO("1",
                TimetableAgentModelProposalStatus.PROPOSED, "snapshot-1",
                List.of(new TimetableAgentProposalEntryDTO(11L, 21L, null,
                        LocalDate.of(2026, 10, 5), LocalDate.of(2026, 12, 31))),
                List.of(), "Đề xuất hợp lệ");
        Assertions.assertEquals(expected, result,
                "strict decoding must preserve the full proposal and nullable room");
    }

    @Test
    void proposeRejectsUnknownRootAndNestedFields() {
        String unknownRoot = VALID_PROPOSAL.replace("\"explanation\":",
                "\"modelAdvice\":\"ignore guards\",\"explanation\":");
        String unknownEntry = VALID_PROPOSAL.replace("\"periodId\":21,", "\"periodId\":21,\"entryId\":99,");

        List.of(unknownRoot, unknownEntry).forEach(this::assertRejected);
    }

    @Test
    void proposeRejectsInvalidStatusAndCalendarDates() {
        String invalidStatus = VALID_PROPOSAL.replace("PROPOSED", "SAVED_DRAFT");
        String invalidDate = VALID_PROPOSAL.replace("2026-10-05", "2026-02-30");

        List.of(invalidStatus, invalidDate).forEach(this::assertRejected);
    }

    @Test
    void proposeRejectsCoercedStringIdsAndNonIntegerNumbers() {
        String stringId = VALID_PROPOSAL.replace("\"assignmentId\":11", "\"assignmentId\":\"11\"");
        String fractionalId = VALID_PROPOSAL.replace("\"assignmentId\":11", "\"assignmentId\":11.5");

        List.of(stringId, fractionalId).forEach(this::assertRejected);
    }

    @Test
    void proposeRejectsEmptyAndMalformedOutput() {
        List.of("  ", "{\"schemaVersion\":").forEach(this::assertRejected);
    }

    @Test
    void proposeRejectsDuplicateKeysAndTrailingJsonValues() {
        String duplicateRootKey = VALID_PROPOSAL.replace("\"schemaVersion\":\"1\",",
                "\"schemaVersion\":\"1\",\"schemaVersion\":\"1\",");
        String duplicateNestedKey = VALID_PROPOSAL.replace("\"periodId\":21,",
                "\"periodId\":21,\"periodId\":21,");
        String trailingValue = VALID_PROPOSAL + " {}";

        List.of(duplicateRootKey, duplicateNestedKey, trailingValue).forEach(this::assertRejected);
    }

    @Test
    void proposeFailsClosedWhenNativeStructuredOutputIsUnsupported() {
        ChatModel chatModel = Mockito.mock(ChatModel.class);
        Mockito.when(chatModel.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        SpringAiTimetableModelGateway gateway =
                new SpringAiTimetableModelGateway(chatModel, new ObjectMapper().findAndRegisterModules());

        Assertions.assertThrows(TimetableAgentModelException.class, () -> gateway.propose(prompt()),
                "proposal generation requires native structured output support");
        Mockito.verify(chatModel, Mockito.never()).call(ArgumentMatchers.any(Prompt.class));
    }

    @Test
    void proposeRejectsRefusedAndTruncatedGenerations() {
        List.of("refusal", "length").forEach(reason -> {
            ChatGenerationMetadata metadata = ChatGenerationMetadata.builder().finishReason(reason).build();
            ChatResponse response = new ChatResponse(List.of(
                    new Generation(new AssistantMessage(VALID_PROPOSAL), metadata)));
            Assertions.assertThrows(TimetableAgentModelException.class,
                    () -> gatewayReturning(response).propose(prompt()),
                    "refused or truncated provider responses must fail closed");
        });
    }

    @Test
    void proposeRejectsMultipleGenerations() {
        ChatResponse response = new ChatResponse(List.of(
                new Generation(new AssistantMessage(VALID_PROPOSAL)),
                new Generation(new AssistantMessage(VALID_PROPOSAL))));
        Assertions.assertThrows(TimetableAgentModelException.class,
                () -> gatewayReturning(response).propose(prompt()),
                "provider responses with multiple generations must fail closed");
    }

    private void assertRejected(String content) {
        Assertions.assertThrows(TimetableAgentModelException.class,
                () -> gatewayReturning(content).propose(prompt()),
                "invalid provider output must fail closed");
    }

    private SpringAiTimetableModelGateway gatewayReturning(String content) {
        return gatewayReturning(new ChatResponse(List.of(new Generation(new AssistantMessage(content)))));
    }

    private SpringAiTimetableModelGateway gatewayReturning(ChatResponse response) {
        ChatModel chatModel = Mockito.mock(ChatModel.class);
        Mockito.when(chatModel.getOptions()).thenReturn(StructuredOutputChatOptions.builder().build());
        Mockito.when(chatModel.call(ArgumentMatchers.any(Prompt.class))).thenReturn(response);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        return new SpringAiTimetableModelGateway(chatModel, objectMapper);
    }

    private TimetableAgentProposalPrompt prompt() {
        return new TimetableAgentProposalPrompt("system", "{}", "user request", "{}");
    }
}

