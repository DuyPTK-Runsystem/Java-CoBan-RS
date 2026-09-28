package com.JavaTraining.BaiTap_RS.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(name = "app.seed.demo.enabled", havingValue = "true")
public class DemoSeedCompletionRunner implements ApplicationRunner {

    private final DemoSeedCompletion completion;
    private final String targetId;
    private final String deploymentRef;

    public DemoSeedCompletionRunner(
            DemoSeedCompletion completion,
            @Value("${app.seed.demo.target-id:local}") String targetId,
            @Value("${app.seed.demo.deployment-ref:local}") String deploymentRef) {
        this.completion = completion;
        this.targetId = targetId;
        this.deploymentRef = deploymentRef;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!completion.isComplete()) {
            completion.markComplete(targetId, deploymentRef);
        }
    }
}
