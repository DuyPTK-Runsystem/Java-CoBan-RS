-- Plan 038: score change request workflow.
CREATE TABLE score_change_request (
    request_id BIGINT NOT NULL AUTO_INCREMENT,
    assessment_column_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    student_score_id BIGINT NULL,
    before_status VARCHAR(20) NOT NULL,
    before_value DECIMAL(3,1) NULL,
    proposed_status VARCHAR(20) NOT NULL,
    proposed_value DECIMAL(3,1) NULL,
    reason VARCHAR(1000) NOT NULL,
    requested_by BIGINT NOT NULL,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    rejection_reason VARCHAR(1000) NULL,
    applied_at TIMESTAMP NULL,
    pending_request_key VARCHAR(100) NULL,
    PRIMARY KEY (request_id),
    CONSTRAINT ck_score_change_before_status CHECK (
        before_status IN ('UNSCORED', 'SCORED', 'ABSENT', 'EXEMPTED', 'CANCELLED')
    ),
    CONSTRAINT ck_score_change_before_value CHECK (
        (before_status = 'SCORED' AND before_value IS NOT NULL
            AND before_value >= 0.0 AND before_value <= 10.0)
        OR (before_status <> 'SCORED' AND before_value IS NULL)
    ),
    CONSTRAINT ck_score_change_proposed_status CHECK (
        proposed_status IN ('SCORED', 'ABSENT', 'EXEMPTED', 'CANCELLED')
    ),
    CONSTRAINT ck_score_change_proposed_value CHECK (
        (proposed_status = 'SCORED' AND proposed_value IS NOT NULL
            AND proposed_value >= 0.0 AND proposed_value <= 10.0)
        OR (proposed_status <> 'SCORED' AND proposed_value IS NULL)
    ),
    CONSTRAINT ck_score_change_status CHECK (
        status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'APPLIED')
    ),
    CONSTRAINT uk_score_change_pending_cell UNIQUE (pending_request_key),
    CONSTRAINT fk_score_change_column FOREIGN KEY (assessment_column_id)
        REFERENCES assessment_column (assessment_column_id),
    CONSTRAINT fk_score_change_student FOREIGN KEY (student_id)
        REFERENCES student (student_id),
    CONSTRAINT fk_score_change_score FOREIGN KEY (student_score_id)
        REFERENCES student_score (score_id),
    CONSTRAINT fk_score_change_requested_by FOREIGN KEY (requested_by)
        REFERENCES app_user (user_id),
    CONSTRAINT fk_score_change_reviewed_by FOREIGN KEY (reviewed_by)
        REFERENCES app_user (user_id)
);

CREATE INDEX idx_score_change_column_student
    ON score_change_request (assessment_column_id, student_id);
CREATE INDEX idx_score_change_requested_by
    ON score_change_request (requested_by);
CREATE INDEX idx_score_change_status
    ON score_change_request (status);
