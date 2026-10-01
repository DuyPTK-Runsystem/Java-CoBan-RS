CREATE TABLE timetable_agent_proposal (
    proposal_id BIGINT NOT NULL AUTO_INCREMENT,
    actor_id BIGINT NOT NULL,
    target_revision_id BIGINT NOT NULL,
    target_semester_id BIGINT NOT NULL,
    expected_version BIGINT NOT NULL,
    proposal_version BIGINT NOT NULL,
    snapshot_id VARCHAR(64) NOT NULL,
    snapshot_fingerprint CHAR(64) NOT NULL,
    proposal_hash CHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL,
    snapshot_json LONGTEXT NOT NULL,
    request_json LONGTEXT NOT NULL,
    proposal_json LONGTEXT NOT NULL,
    validation_json LONGTEXT NOT NULL,
    issues_json LONGTEXT NOT NULL,
    diff_json LONGTEXT NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (proposal_id),
    KEY idx_tap_actor_created (actor_id, created_at),
    KEY idx_tap_revision_status (target_revision_id, status),
    CONSTRAINT fk_tap_actor FOREIGN KEY (actor_id) REFERENCES app_user (user_id),
    CONSTRAINT fk_tap_revision FOREIGN KEY (target_revision_id) REFERENCES timetable_revision (revision_id),
    CONSTRAINT fk_tap_semester FOREIGN KEY (target_semester_id) REFERENCES semester (semester_id)
);

CREATE TABLE timetable_agent_approval (
    approval_id BIGINT NOT NULL AUTO_INCREMENT,
    proposal_id BIGINT NOT NULL,
    proposal_version BIGINT NOT NULL,
    proposal_hash CHAR(64) NOT NULL,
    actor_id BIGINT NOT NULL,
    approved_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (approval_id),
    UNIQUE KEY uk_taa_proposal_version (proposal_id, proposal_version),
    CONSTRAINT fk_taa_proposal FOREIGN KEY (proposal_id) REFERENCES timetable_agent_proposal (proposal_id),
    CONSTRAINT fk_taa_actor FOREIGN KEY (actor_id) REFERENCES app_user (user_id)
);

CREATE TABLE timetable_agent_action (
    action_id BIGINT NOT NULL AUTO_INCREMENT,
    proposal_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL,
    lease_token CHAR(36) NOT NULL,
    lease_expires_at TIMESTAMP(6) NOT NULL,
    error_code VARCHAR(64),
    receipt_json LONGTEXT,
    committed_at TIMESTAMP(6),
    PRIMARY KEY (action_id),
    UNIQUE KEY uk_taa_actor_idempotency (actor_id, idempotency_key),
    KEY idx_taa_proposal (proposal_id),
    CONSTRAINT fk_tact_proposal FOREIGN KEY (proposal_id) REFERENCES timetable_agent_proposal (proposal_id),
    CONSTRAINT fk_tact_actor FOREIGN KEY (actor_id) REFERENCES app_user (user_id)
);
