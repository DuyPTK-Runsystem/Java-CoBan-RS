INSERT INTO role (code, name, description)
VALUES ('LIBRARIAN', 'Librarian', 'Quản lý nghiệp vụ thư viện')
ON DUPLICATE KEY UPDATE name = VALUES(name);

CREATE TABLE library_patron (
    patron_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    joined_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (patron_id),
    UNIQUE KEY uk_library_patron_user (user_id),
    KEY idx_library_patron_status (status, patron_id),
    CONSTRAINT fk_library_patron_user FOREIGN KEY (user_id) REFERENCES app_user (user_id)
);

CREATE TABLE library_patron_suspension (
    suspension_id BIGINT NOT NULL AUTO_INCREMENT,
    patron_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    source VARCHAR(32) NOT NULL,
    suspended_at DATETIME(6) NOT NULL,
    resolved_at DATETIME(6) NULL,
    resolved_by BIGINT NULL,
    PRIMARY KEY (suspension_id),
    KEY idx_library_suspension_active (patron_id, resolved_at),
    CONSTRAINT fk_library_suspension_patron FOREIGN KEY (patron_id) REFERENCES library_patron (patron_id),
    CONSTRAINT fk_library_suspension_resolver FOREIGN KEY (resolved_by) REFERENCES app_user (user_id)
);

CREATE TABLE library_card (
    card_id BIGINT NOT NULL AUTO_INCREMENT,
    patron_id BIGINT NOT NULL,
    card_no VARCHAR(32) NOT NULL,
    issued_at DATETIME(6) NOT NULL,
    expires_at DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    payload_version VARCHAR(8) NOT NULL,
    policy_version VARCHAR(64) NOT NULL,
    revoked_at DATETIME(6) NULL,
    revoked_reason VARCHAR(500) NULL,
    active_patron_id BIGINT GENERATED ALWAYS AS (CASE WHEN status = 'ACTIVE' THEN patron_id ELSE NULL END) STORED,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (card_id),
    UNIQUE KEY uk_library_card_no (card_no),
    UNIQUE KEY uk_library_card_active_patron (active_patron_id),
    KEY idx_library_card_patron_history (patron_id, issued_at),
    CONSTRAINT fk_library_card_patron FOREIGN KEY (patron_id) REFERENCES library_patron (patron_id)
);

CREATE TABLE library_card_sequence (
    sequence_year INT NOT NULL,
    sequence_value INT NOT NULL,
    PRIMARY KEY (sequence_year)
);
