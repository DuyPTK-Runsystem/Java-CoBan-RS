package com.JavaTraining.BaiTap_RS.timetableagent.ai;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimetableAgentModelGatewayResolver {

    private final ObjectProvider<TimetableAgentModelGateway> gatewayProvider;
    private final ObjectProvider<ChatModel> chatModelProvider;
    private final ObjectMapper objectMapper;

    public TimetableAgentModelGateway getIfUnambiguous() {
        List<ChatModel> chatModels = chatModelProvider.orderedStream().toList();
        if (chatModels.size() > 1) {
            return null;
        }
        List<TimetableAgentModelGateway> gateways = gatewayProvider.orderedStream().toList();
        if (gateways.size() > 1) {
            return null;
        }
        if (gateways.size() == 1) {
            return gateways.getFirst();
        }
        return chatModels.size() == 1 ? new SpringAiTimetableModelGateway(chatModels.getFirst(), objectMapper) : null;
    }
}
