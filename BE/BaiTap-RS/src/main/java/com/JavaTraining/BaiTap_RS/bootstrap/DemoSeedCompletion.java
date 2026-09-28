package com.JavaTraining.BaiTap_RS.bootstrap;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DemoSeedCompletion {

    /* default */ static final String SEED_KEY = "DEMO_FIXTURE_PLAN_081";
    /* default */ static final String FIXTURE_VERSION = "PLAN_081_V1";

    private final JdbcTemplate jdbcTemplate;

    public DemoSeedCompletion(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isComplete() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_demo_seed_completion WHERE seed_key = ?",
                Integer.class,
                SEED_KEY);
        return count != null && count > 0;
    }

    public void markComplete(String targetId, String deploymentRef) {
        jdbcTemplate.update(
                "INSERT INTO app_demo_seed_completion "
                        + "(seed_key, fixture_version, target_id, deployment_ref) VALUES (?, ?, ?, ?)",
                SEED_KEY,
                FIXTURE_VERSION,
                targetId,
                deploymentRef);
    }
}
