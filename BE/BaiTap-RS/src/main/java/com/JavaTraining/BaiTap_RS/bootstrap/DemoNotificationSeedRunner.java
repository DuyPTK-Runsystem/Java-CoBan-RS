package com.JavaTraining.BaiTap_RS.bootstrap;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(500)
@ConditionalOnProperty(name = "app.seed.demo.enabled", havingValue = "true")
public class DemoNotificationSeedRunner implements ApplicationRunner {

    private final DemoSeedCompletion completion;
    private final DemoNotificationSeeder notificationSeeder;

    public DemoNotificationSeedRunner(
            DemoSeedCompletion completion,
            DemoNotificationSeeder notificationSeeder) {
        this.completion = completion;
        this.notificationSeeder = notificationSeeder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!completion.isComplete()) {
            notificationSeeder.run(args);
        }
    }
}
