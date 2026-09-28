package com.JavaTraining.BaiTap_RS.bootstrap;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class DemoSeedCompletionTest {

    private static final String TARGET_ID = "demo-mysql";
    private static final String DEPLOYMENT_REF = "sha-abc123";

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void recordsMarkerOnFirstSuccessfulCompletion() {
        DemoSeedCompletion completion = new DemoSeedCompletion(jdbcTemplate);
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_demo_seed_completion WHERE seed_key = ?",
                Integer.class,
                DemoSeedCompletion.SEED_KEY)).thenReturn(0);
        DemoSeedCompletionRunner runner = new DemoSeedCompletionRunner(completion, TARGET_ID, DEPLOYMENT_REF);

        runner.run(new DefaultApplicationArguments());

        verify(jdbcTemplate).update(
                "INSERT INTO app_demo_seed_completion "
                        + "(seed_key, fixture_version, target_id, deployment_ref) VALUES (?, ?, ?, ?)",
                DemoSeedCompletion.SEED_KEY,
                DemoSeedCompletion.FIXTURE_VERSION,
                TARGET_ID,
                DEPLOYMENT_REF);
    }

    @Test
    void doesNotRewriteMarkerWhenFixtureIsAlreadyComplete() {
        DemoSeedCompletion completion = new DemoSeedCompletion(jdbcTemplate);
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_demo_seed_completion WHERE seed_key = ?",
                Integer.class,
                DemoSeedCompletion.SEED_KEY)).thenReturn(1);
        DemoSeedCompletionRunner runner = new DemoSeedCompletionRunner(completion, TARGET_ID, DEPLOYMENT_REF);

        runner.run(new DefaultApplicationArguments());

        verify(jdbcTemplate, never()).update(
                org.mockito.ArgumentMatchers.contains("INSERT INTO app_demo_seed_completion"),
                org.mockito.ArgumentMatchers.<Object[]>any());
    }

    @Test
    void propagatesMarkerPersistenceFailureWithoutReportingCompletion() {
        DemoSeedCompletion completion = new DemoSeedCompletion(jdbcTemplate);
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_demo_seed_completion WHERE seed_key = ?",
                Integer.class,
                DemoSeedCompletion.SEED_KEY)).thenReturn(0);
        doThrow(new DataAccessResourceFailureException("database unavailable"))
                .when(jdbcTemplate).update(
                        "INSERT INTO app_demo_seed_completion "
                                + "(seed_key, fixture_version, target_id, deployment_ref) VALUES (?, ?, ?, ?)",
                        DemoSeedCompletion.SEED_KEY,
                        DemoSeedCompletion.FIXTURE_VERSION,
                        TARGET_ID,
                        DEPLOYMENT_REF);
        DemoSeedCompletionRunner runner = new DemoSeedCompletionRunner(completion, TARGET_ID, DEPLOYMENT_REF);

        assertThrows(DataAccessResourceFailureException.class, () -> runner.run(new DefaultApplicationArguments()));
    }
}
