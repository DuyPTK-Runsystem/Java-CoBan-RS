package com.JavaTraining.BaiTap_RS.timetableagent.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SpringAiTimetableModelConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SpringAiTimetableModelConfiguration.class)
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    void noModelMeansNoGatewayBean() {
        contextRunner.run(context -> Assertions.assertFalse(context.containsBean("timetableAgentModelGateway"),
                "the Spring gateway requires a configured ChatModel"));
    }

    @Test
    void oneModelCreatesExactlyOneGatewayBean() {
        contextRunner.withBean("chatModel", ChatModel.class, () -> Mockito.mock(ChatModel.class))
                .run(context -> Assertions.assertTrue(context.getBean(TimetableAgentModelGateway.class)
                        instanceof SpringAiTimetableModelGateway,
                        "one configured ChatModel must create the provider-neutral gateway"));
    }

    @Test
    void multipleModelsDoNotChooseAnArbitraryGateway() {
        contextRunner.withBean("chatModelOne", ChatModel.class, () -> Mockito.mock(ChatModel.class))
                .withBean("chatModelTwo", ChatModel.class, () -> Mockito.mock(ChatModel.class))
                .run(context -> Assertions.assertFalse(context.containsBean("timetableAgentModelGateway"),
                        "ambiguous ChatModel beans must not create an arbitrary gateway"));
    }
}
