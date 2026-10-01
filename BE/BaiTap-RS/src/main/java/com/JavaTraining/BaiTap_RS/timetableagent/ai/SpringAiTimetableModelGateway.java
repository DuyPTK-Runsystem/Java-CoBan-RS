package com.JavaTraining.BaiTap_RS.timetableagent.ai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.TimetableAgentModelProposalDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.ai.model.tool.StructuredOutputChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.core.io.ClassPathResource;

public class SpringAiTimetableModelGateway implements TimetableAgentModelGateway {

    private static final String PROPOSAL_SCHEMA_PATH = "ai/timetable/timetable-proposal.schema.json";
    private static final String ACTION_SCHEMA_PATH = "ai/timetable/save-timetable-draft.schema.json";
    private static final String SAVE_TOOL_NAME = "saveTimetableDraft";
    private static final String SAVE_TOOL_DESCRIPTION =
            "Request the application to save the exact user-approved timetable draft.";
    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public SpringAiTimetableModelGateway(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.chatClient = ChatClient.builder(chatModel).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public TimetableAgentModelProposalDTO propose(TimetableAgentProposalPrompt prompt) {
        if (!supportsNativeStructuredOutput()) {
            throw new TimetableAgentModelException(
                    "Configured model does not support isolated native structured output.");
        }
        String schema = readResource(PROPOSAL_SCHEMA_PATH);
        String userContent = "SNAPSHOT (JSON):\n" + prompt.snapshotJson()
                + "\n\nUSER_REQUEST (DATA):\n" + prompt.userRequest();
        StructuredOutputConverter<TimetableAgentModelProposalDTO> converter =
                new TimetableAgentProposalConverter(objectMapper, schema);
        long started = System.nanoTime();
        ChatResponse response = null;
        String outcome = "FAILED";
        try {
            response = chatClient.prompt()
                    .messages(new SystemMessage(prompt.systemInstructions()), new UserMessage(userContent))
                    .options(((StructuredOutputChatOptions) chatModel.getOptions()).mutate()
                            .outputSchema(converter.getJsonSchema()))
                    .advisors(AdvisorParams.toolCallingAdvisorAutoRegister(false))
                    .call()
                    .chatResponse();
            requireSingleCompleteGeneration(response);
            if (response.getResult().getOutput().getToolCalls() != null
                    && !response.getResult().getOutput().getToolCalls().isEmpty()) {
                throw new TimetableAgentModelException("Proposal phase must not return tool calls.");
            }
            TimetableAgentModelProposalDTO proposal = converter.convert(response.getResult().getOutput().getText());
            outcome = "SUCCESS";
            return proposal;
        } finally {
            TimetableAgentModelUsage.from(response).record("proposal", outcome, System.nanoTime() - started);
        }
    }

    @Override
    public boolean supportsNativeStructuredOutput() {
        return chatModel.getOptions() instanceof StructuredOutputChatOptions && hasNoDefaultTools();
    }

    @Override
    public boolean supportsSaveToolCalling() {
        return chatModel.getOptions() instanceof ToolCallingChatOptions && hasNoDefaultTools();
    }

    private boolean hasNoDefaultTools() {
        if (chatModel.getOptions() instanceof ToolCallingChatOptions options) {
            return options.getToolCallbacks() == null || options.getToolCallbacks().isEmpty();
        }
        return true;
    }

    @Override
    public TimetableAgentToolCall requestSave(TimetableAgentActionPrompt prompt) {
        if (!supportsSaveToolCalling()) {
            throw new TimetableAgentModelException("Configured model does not support isolated native tool calling.");
        }
        String schema = readResource(ACTION_SCHEMA_PATH);
        ToolCallback guardedCallback = new ToolCallback() {
            private final ToolDefinition definition = ToolDefinition.builder()
                    .name(SAVE_TOOL_NAME)
                    .description(SAVE_TOOL_DESCRIPTION)
                    .inputSchema(schema)
                    .build();

            @Override
            public ToolDefinition getToolDefinition() {
                return definition;
            }

            @Override
            public ToolMetadata getToolMetadata() {
                return ToolMetadata.builder().build();
            }

            @Override
            public String call(String toolInput) {
                throw new IllegalStateException("Timetable action tools are dispatched only by the application.");
            }
        };

        long started = System.nanoTime();
        ChatResponse response = null;
        String outcome = "FAILED";
        try {
            response = chatClient.prompt()
                    .messages(new SystemMessage(prompt.systemInstructions()), new UserMessage(
                            "APPROVED_PROPOSAL_REFERENCE (JSON):\n" + prompt.approvedProposalReference()))
                    .options(ToolCallingChatOptions.builder().toolCallbacks(guardedCallback))
                    .advisors(AdvisorParams.toolCallingAdvisorAutoRegister(false))
                    .call()
                    .chatResponse();
            requireSingleCompleteGeneration(response);
            List<org.springframework.ai.chat.messages.AssistantMessage.ToolCall> calls = response.getResult()
                    .getOutput().getToolCalls();
            if (calls == null || calls.size() != 1) {
                return null;
            }
            org.springframework.ai.chat.messages.AssistantMessage.ToolCall call = calls.get(0);
            outcome = "SUCCESS";
            return new TimetableAgentToolCall(call.id(), call.name(), call.arguments());
        } finally {
            TimetableAgentModelUsage.from(response).record("action", outcome, System.nanoTime() - started);
        }
    }

    private void requireSingleCompleteGeneration(ChatResponse response) {
        if (response == null || response.getResults().size() != 1 || response.getResult().getOutput() == null) {
            throw new TimetableAgentModelException("Model must return exactly one complete generation.");
        }
        String finishReason = response.getResult().getMetadata() == null ? null
                : response.getResult().getMetadata().getFinishReason();
        if (finishReason != null && Set.of("length", "max_tokens", "content_filter", "refusal")
                .contains(finishReason.toLowerCase(java.util.Locale.ROOT))) {
            throw new TimetableAgentModelException("Model output was truncated or refused.");
        }
    }

    private String readResource(String path) {
        try (InputStream inputStream = new ClassPathResource(path).getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new TimetableAgentModelException("Required timetable AI contract resource is unavailable.",
                    exception);
        }
    }
}
