CREATE TABLE placement_session (
    placement_session_id BIGINT NOT NULL AUTO_INCREMENT,
    academic_year_id BIGINT NOT NULL,
    target_grade_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    rule_version VARCHAR(50) NOT NULL,
    scope_snapshot JSON NOT NULL,
    expected_version BIGINT NOT NULL DEFAULT 0,
    confirm_idempotency_key VARCHAR(100) NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (placement_session_id),
    CONSTRAINT uk_placement_session_idempotency UNIQUE (confirm_idempotency_key),
    CONSTRAINT fk_placement_session_year FOREIGN KEY (academic_year_id) REFERENCES academic_year (academic_year_id),
    CONSTRAINT fk_placement_session_grade FOREIGN KEY (target_grade_id) REFERENCES grade_level (grade_level_id),
    CONSTRAINT fk_placement_session_actor FOREIGN KEY (created_by) REFERENCES app_user (user_id)
);

CREATE INDEX idx_placement_session_scope ON placement_session (academic_year_id, target_grade_id, status);

CREATE TABLE placement_candidate (
    placement_candidate_id BIGINT NOT NULL AUTO_INCREMENT,
    placement_session_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    target_grade_id BIGINT NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    score DECIMAL(8,3) NULL,
    score_source_reference VARCHAR(255) NULL,
    gender_snapshot VARCHAR(50) NULL,
    eligibility_evidence JSON NULL,
    approval_reference VARCHAR(255) NULL,
    PRIMARY KEY (placement_candidate_id),
    CONSTRAINT uk_placement_candidate_student UNIQUE (placement_session_id, student_id),
    CONSTRAINT fk_placement_candidate_session FOREIGN KEY (placement_session_id) REFERENCES placement_session (placement_session_id),
    CONSTRAINT fk_placement_candidate_student FOREIGN KEY (student_id) REFERENCES student (student_id),
    CONSTRAINT fk_placement_candidate_grade FOREIGN KEY (target_grade_id) REFERENCES grade_level (grade_level_id)
);

CREATE TABLE placement_result (
    placement_result_id BIGINT NOT NULL AUTO_INCREMENT,
    placement_session_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    target_class_id BIGINT NULL,
    result_status VARCHAR(30) NOT NULL,
    score DECIMAL(8,3) NULL,
    issue_code VARCHAR(60) NULL,
    issue_severity VARCHAR(20) NULL,
    explanation VARCHAR(1000) NOT NULL,
    result_version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (placement_result_id),
    CONSTRAINT uk_placement_result_student UNIQUE (placement_session_id, student_id),
    CONSTRAINT fk_placement_result_session FOREIGN KEY (placement_session_id) REFERENCES placement_session (placement_session_id),
    CONSTRAINT fk_placement_result_student FOREIGN KEY (student_id) REFERENCES student (student_id),
    CONSTRAINT fk_placement_result_class FOREIGN KEY (target_class_id) REFERENCES school_class (class_id)
);

CREATE INDEX idx_placement_result_session_status ON placement_result (placement_session_id, result_status);
