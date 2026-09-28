package com.JavaTraining.BaiTap_RS.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
class DemoNotificationSeedRunnerTest {

    @Mock
    private DemoSeedCompletion completion;

    @Mock
    private DemoNotificationSeeder notificationSeeder;

    @Test
    void skipsNotificationSeederWhenCompletionMarkerExists() {
        when(completion.isComplete()).thenReturn(true);
        DemoNotificationSeedRunner runner = new DemoNotificationSeedRunner(completion, notificationSeeder);

        runner.run(new DefaultApplicationArguments());

        verify(notificationSeeder, never()).run(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void runsNotificationSeederWhenCompletionMarkerIsAbsent() {
        when(completion.isComplete()).thenReturn(false);
        DemoNotificationSeedRunner runner = new DemoNotificationSeedRunner(completion, notificationSeeder);
        DefaultApplicationArguments arguments = new DefaultApplicationArguments();

        runner.run(arguments);

        verify(notificationSeeder).run(arguments);
    }
}
