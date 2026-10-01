package com.JavaTraining.BaiTap_RS.timetableagent.ai;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(ChatModel.class)
public class SpringAiTimetableModelConfiguration {

    @Bean
    @ConditionalOnSingleCandidate(ChatModel.class)
    @ConditionalOnMissingBean(TimetableAgentModelGateway.class)
    public TimetableAgentModelGateway timetableAgentModelGateway(List<ChatModel> chatModels,
            ObjectMapper objectMapper) {
        return new SpringAiTimetableModelGateway(chatModels.getFirst(), objectMapper);
    }
}
