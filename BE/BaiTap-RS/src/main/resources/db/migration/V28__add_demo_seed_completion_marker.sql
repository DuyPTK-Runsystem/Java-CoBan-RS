-- Persist successful completion of the versioned demo fixture bootstrap.
CREATE TABLE app_demo_seed_completion (
    seed_key VARCHAR(80) NOT NULL,
    fixture_version VARCHAR(40) NOT NULL,
    target_id VARCHAR(128) NOT NULL,
    deployment_ref VARCHAR(255) NOT NULL,
    completed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_app_demo_seed_completion PRIMARY KEY (seed_key)
);
