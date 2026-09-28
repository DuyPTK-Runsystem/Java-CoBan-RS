package com.JavaTraining.BaiTap_RS.bootstrap;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

class DemoSeedActivationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SeedRunnerConfiguration.class);

    @Test
    void missingEnvironmentFlagUsesDisabledApplicationDefault() {
        contextRunner.run(context -> assertTrue(
                context.getBeansOfType(ApplicationRunner.class).isEmpty(),
                "no demo runner should be registered when APP_SEED_DEMO_ENABLED is absent"));
    }

    @Test
    void explicitFalseDisablesAllDemoRunners() {
        contextRunner.withPropertyValues("APP_SEED_DEMO_ENABLED=false").run(context -> assertTrue(
                context.getBeansOfType(ApplicationRunner.class).isEmpty(),
                "no demo runner should be registered when APP_SEED_DEMO_ENABLED=false"));
    }

    @Configuration(proxyBeanMethods = false)
    @PropertySource("classpath:application.properties")
    @Import({
            DemoDataSeeder.class,
            DemoFunctionalRoomSeeder.class,
            DemoPlacementSeeder.class,
            DemoScorebookSeeder.class,
            DemoNotificationSeedRunner.class,
            DemoSeedCompletionRunner.class
    })
    static class SeedRunnerConfiguration {
    }
}
