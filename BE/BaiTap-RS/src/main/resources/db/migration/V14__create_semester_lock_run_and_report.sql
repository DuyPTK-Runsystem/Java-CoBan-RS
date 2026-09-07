-- Plan 039: semester lock run and completeness report

ALTER TABLE semester ADD COLUMN reopen_until TIMESTAMP NULL;

CREATE TABLE semester_lock_run (
    run_id BIGINT NOT NULL AUTO_INCREMENT,
    business_date DATE NOT NULL,
    batch_execution_id BIGINT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP NULL,
    last_error VARCHAR(2000) NULL,
    PRIMARY KEY (run_id),
    CONSTRAINT ck_semester_lock_run_status CHECK (
        status IN ('RUNNING', 'SUCCEEDED', 'FAILED')
    )
);

CREATE TABLE semester_lock_report (
    report_id BIGINT NOT NULL AUTO_INCREMENT,
    run_id BIGINT NOT NULL,
    semester_id BIGINT NOT NULL,
    checkpoint_code VARCHAR(30) NOT NULL,
    report_status VARCHAR(20) NOT NULL,
    evaluated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    scope_type VARCHAR(30) NOT NULL,
    summary_payload TEXT NOT NULL,
    failure_reason VARCHAR(2000) NULL,
    correlation_id VARCHAR(100) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (report_id),
    CONSTRAINT ck_semester_lock_report_status CHECK (
        report_status IN ('COMPLETE', 'INCOMPLETE', 'FAILED')
    ),
    CONSTRAINT uk_lock_report_run_sem_chk UNIQUE (run_id, semester_id, checkpoint_code),
    CONSTRAINT fk_semester_lock_report_run FOREIGN KEY (run_id)
        REFERENCES semester_lock_run (run_id),
    CONSTRAINT fk_semester_lock_report_semester FOREIGN KEY (semester_id)
        REFERENCES semester (semester_id)
);

CREATE INDEX idx_lock_report_semester_chk
    ON semester_lock_report (semester_id, checkpoint_code);
CREATE INDEX idx_lock_run_business_date
    ON semester_lock_run (business_date);

