CREATE TABLE library_circulation_policy (
    policy_id BIGINT NOT NULL AUTO_INCREMENT,
    policy_version VARCHAR(64) NOT NULL,
    effective_at DATETIME(6) NOT NULL,
    max_active_loans INT NOT NULL,
    loan_duration_days INT NOT NULL,
    max_renewals INT NOT NULL,
    renewal_duration_days INT NOT NULL,
    reservation_pickup_days INT NOT NULL,
    fine_cap_per_loan DECIMAL(12,2) NOT NULL,
    fine_suspension_threshold DECIMAL(12,2) NOT NULL,
    updated_by BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (policy_id),
    UNIQUE KEY uk_library_policy_version (policy_version),
    KEY idx_library_policy_effective (effective_at, policy_id),
    CONSTRAINT fk_library_policy_actor FOREIGN KEY (updated_by) REFERENCES app_user (user_id)
);

CREATE TABLE library_policy_fine_tier (
    fine_tier_id BIGINT NOT NULL AUTO_INCREMENT,
    policy_id BIGINT NOT NULL,
    tier_order INT NOT NULL,
    through_day INT NULL,
    rate_per_day DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (fine_tier_id),
    UNIQUE KEY uk_library_fine_tier_order (policy_id, tier_order),
    CONSTRAINT fk_library_fine_tier_policy FOREIGN KEY (policy_id) REFERENCES library_circulation_policy (policy_id)
);

INSERT INTO library_circulation_policy
    (policy_version, effective_at, max_active_loans, loan_duration_days, max_renewals,
     renewal_duration_days, reservation_pickup_days, fine_cap_per_loan, fine_suspension_threshold,
     updated_by, updated_at)
SELECT 'LIB-POLICY-1', '1970-01-01 00:00:00', 5, 14, 2, 7, 3, 500000.00, 500000.00,
       u.user_id, CURRENT_TIMESTAMP(6)
FROM app_user u ORDER BY u.user_id LIMIT 1;

INSERT INTO library_policy_fine_tier (policy_id, tier_order, through_day, rate_per_day)
SELECT policy_id, 1, 7, 5000.00 FROM library_circulation_policy WHERE policy_version = 'LIB-POLICY-1';
INSERT INTO library_policy_fine_tier (policy_id, tier_order, through_day, rate_per_day)
SELECT policy_id, 2, 30, 10000.00 FROM library_circulation_policy WHERE policy_version = 'LIB-POLICY-1';
INSERT INTO library_policy_fine_tier (policy_id, tier_order, through_day, rate_per_day)
SELECT policy_id, 3, NULL, 20000.00 FROM library_circulation_policy WHERE policy_version = 'LIB-POLICY-1';

CREATE TABLE library_loan (
    loan_id BIGINT NOT NULL AUTO_INCREMENT,
    patron_id BIGINT NOT NULL,
    copy_id BIGINT NOT NULL,
    card_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    borrowed_at DATETIME(6) NOT NULL,
    due_at DATETIME(6) NOT NULL,
    returned_at DATETIME(6) NULL,
    lost_at DATETIME(6) NULL,
    renew_count INT NOT NULL DEFAULT 0,
    policy_version VARCHAR(64) NOT NULL,
    max_active_loans_snapshot INT NOT NULL,
    loan_duration_days_snapshot INT NOT NULL,
    max_renewals_snapshot INT NOT NULL,
    renewal_duration_days_snapshot INT NOT NULL,
    active_copy_id BIGINT GENERATED ALWAYS AS (CASE WHEN status = 'ACTIVE' THEN copy_id ELSE NULL END) STORED,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (loan_id),
    UNIQUE KEY uk_library_loan_active_copy (active_copy_id),
    KEY idx_library_loan_patron_history (patron_id, borrowed_at),
    KEY idx_library_loan_due (status, due_at, loan_id),
    KEY idx_library_loan_copy_status (copy_id, status),
    CONSTRAINT fk_library_loan_patron FOREIGN KEY (patron_id) REFERENCES library_patron (patron_id),
    CONSTRAINT fk_library_loan_copy FOREIGN KEY (copy_id) REFERENCES book_copy (book_copy_id),
    CONSTRAINT fk_library_loan_card FOREIGN KEY (card_id) REFERENCES library_card (card_id),
    CONSTRAINT fk_library_loan_policy FOREIGN KEY (policy_version) REFERENCES library_circulation_policy (policy_version)
);


CREATE TABLE library_loan_renewal (
    renewal_id BIGINT NOT NULL AUTO_INCREMENT,
    loan_id BIGINT NOT NULL,
    renewal_number INT NOT NULL,
    policy_version VARCHAR(64) NOT NULL,
    renewal_days_snapshot INT NOT NULL,
    previous_due_at DATETIME(6) NOT NULL,
    new_due_at DATETIME(6) NOT NULL,
    actor_user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (renewal_id),
    UNIQUE KEY uk_library_renewal_number (loan_id, renewal_number),
    CONSTRAINT fk_library_renewal_loan FOREIGN KEY (loan_id) REFERENCES library_loan (loan_id),
    CONSTRAINT fk_library_renewal_policy FOREIGN KEY (policy_version) REFERENCES library_circulation_policy (policy_version),
    CONSTRAINT fk_library_renewal_actor FOREIGN KEY (actor_user_id) REFERENCES app_user (user_id)
);

CREATE TABLE library_reservation (
    reservation_id BIGINT NOT NULL AUTO_INCREMENT,
    book_id BIGINT NOT NULL,
    patron_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    reserved_at DATETIME(6) NOT NULL,
    ready_at DATETIME(6) NULL,
    pickup_due_at DATETIME(6) NULL,
    allocated_copy_id BIGINT NULL,
    fulfilled_at DATETIME(6) NULL,
    cancelled_at DATETIME(6) NULL,
    expired_at DATETIME(6) NULL,
    policy_version VARCHAR(64) NOT NULL,
    pickup_days_snapshot INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (reservation_id),
    KEY idx_library_reservation_queue (book_id, status, reserved_at, reservation_id),
    KEY idx_library_reservation_patron (patron_id, reserved_at),
    KEY idx_library_reservation_expiry (status, pickup_due_at),
    CONSTRAINT fk_library_reservation_book FOREIGN KEY (book_id) REFERENCES book (book_id),
    CONSTRAINT fk_library_reservation_patron FOREIGN KEY (patron_id) REFERENCES library_patron (patron_id),
    CONSTRAINT fk_library_reservation_copy FOREIGN KEY (allocated_copy_id) REFERENCES book_copy (book_copy_id),
    CONSTRAINT fk_library_reservation_policy FOREIGN KEY (policy_version) REFERENCES library_circulation_policy (policy_version)
);

CREATE TABLE library_fine (
    fine_id BIGINT NOT NULL AUTO_INCREMENT,
    loan_id BIGINT NOT NULL,
    fine_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    provisional BOOLEAN NOT NULL DEFAULT FALSE,
    calculated_through DATE NULL,
    policy_version VARCHAR(64) NOT NULL,
    paid_at DATETIME(6) NULL,
    payment_reference VARCHAR(200) NULL,
    paid_by BIGINT NULL,
    waived_at DATETIME(6) NULL,
    waive_reason VARCHAR(500) NULL,
    waived_by BIGINT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (fine_id),
    UNIQUE KEY uk_library_fine_loan_type (loan_id, fine_type),
    KEY idx_library_fine_status (status, fine_id),
    CONSTRAINT fk_library_fine_loan FOREIGN KEY (loan_id) REFERENCES library_loan (loan_id),
    CONSTRAINT fk_library_fine_policy FOREIGN KEY (policy_version) REFERENCES library_circulation_policy (policy_version),
    CONSTRAINT fk_library_fine_paid_by FOREIGN KEY (paid_by) REFERENCES app_user (user_id),
    CONSTRAINT fk_library_fine_waived_by FOREIGN KEY (waived_by) REFERENCES app_user (user_id)
);

CREATE TABLE library_batch_run_summary (
    run_id BIGINT NOT NULL AUTO_INCREMENT,
    job_name VARCHAR(120) NOT NULL,
    run_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    processed_count BIGINT NOT NULL DEFAULT 0,
    skipped_count BIGINT NOT NULL DEFAULT 0,
    started_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    actor_user_id BIGINT NULL,
    PRIMARY KEY (run_id),
    KEY idx_library_batch_history (job_name, started_at),
    CONSTRAINT fk_library_batch_actor FOREIGN KEY (actor_user_id) REFERENCES app_user (user_id)
);
