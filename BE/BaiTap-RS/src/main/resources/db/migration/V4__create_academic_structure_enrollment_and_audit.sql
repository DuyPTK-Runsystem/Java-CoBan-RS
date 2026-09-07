-- Plan 026: FR-CLASS/FR-ENROLL schema, BR-ENROLL-001 uniqueness and NFR-AUDITABILITY-001 transfer audit.
ALTER TABLE student
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX idx_student_status ON student (status);

CREATE TABLE grade_level (
    grade_level_id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(10) NOT NULL,
    name VARCHAR(50) NOT NULL,
    grade_level INT NOT NULL,
    display_order INT NOT NULL,
    next_grade_id BIGINT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (grade_level_id),
    CONSTRAINT uk_grade_level_code UNIQUE (code),
    CONSTRAINT uk_grade_level_level UNIQUE (grade_level),
    CONSTRAINT fk_grade_level_next_grade FOREIGN KEY (next_grade_id)
        REFERENCES grade_level (grade_level_id)
);

CREATE TABLE academic_year (
    academic_year_id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    notes VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (academic_year_id),
    CONSTRAINT uk_academic_year_code UNIQUE (code),
    CONSTRAINT ck_academic_year_dates CHECK (end_date > start_date),
    CONSTRAINT ck_academic_year_status CHECK (status IN ('DRAFT', 'ACTIVE', 'CLOSED'))
);

CREATE TABLE school_class (
    class_id BIGINT NOT NULL AUTO_INCREMENT,
    academic_year_id BIGINT NOT NULL,
    grade_level_id BIGINT NOT NULL,
    class_code VARCHAR(30) NOT NULL,
    class_name VARCHAR(100) NULL,
    capacity INT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (class_id),
    CONSTRAINT uk_school_class_year_code UNIQUE (academic_year_id, class_code),
    CONSTRAINT ck_school_class_capacity CHECK (capacity IS NULL OR capacity >= 0),
    CONSTRAINT ck_school_class_status CHECK (status IN ('PLANNED', 'ACTIVE', 'CLOSED')),
    CONSTRAINT fk_school_class_year FOREIGN KEY (academic_year_id)
        REFERENCES academic_year (academic_year_id),
    CONSTRAINT fk_school_class_grade FOREIGN KEY (grade_level_id)
        REFERENCES grade_level (grade_level_id)
);

CREATE INDEX idx_school_class_year_grade ON school_class (academic_year_id, grade_level_id);

CREATE TABLE student_year_enrollment (
    enrollment_id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    academic_year_id BIGINT NOT NULL,
    current_class_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    enrolled_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (enrollment_id),
    CONSTRAINT uk_enrollment_student_year UNIQUE (student_id, academic_year_id),
    CONSTRAINT ck_enrollment_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'WITHDRAWN')),
    CONSTRAINT fk_enrollment_student FOREIGN KEY (student_id) REFERENCES student (student_id),
    CONSTRAINT fk_enrollment_year FOREIGN KEY (academic_year_id)
        REFERENCES academic_year (academic_year_id),
    CONSTRAINT fk_enrollment_class FOREIGN KEY (current_class_id)
        REFERENCES school_class (class_id)
);

CREATE INDEX idx_enrollment_year_class_status
    ON student_year_enrollment (academic_year_id, current_class_id, status);

CREATE TABLE class_transfer_history (
    transfer_id BIGINT NOT NULL AUTO_INCREMENT,
    enrollment_id BIGINT NOT NULL,
    from_class_id BIGINT NULL,
    to_class_id BIGINT NOT NULL,
    effective_at TIMESTAMP NOT NULL,
    reason VARCHAR(500) NULL,
    approved_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (transfer_id),
    CONSTRAINT fk_transfer_enrollment FOREIGN KEY (enrollment_id)
        REFERENCES student_year_enrollment (enrollment_id),
    CONSTRAINT fk_transfer_from_class FOREIGN KEY (from_class_id)
        REFERENCES school_class (class_id),
    CONSTRAINT fk_transfer_to_class FOREIGN KEY (to_class_id)
        REFERENCES school_class (class_id),
    CONSTRAINT fk_transfer_approved_by FOREIGN KEY (approved_by)
        REFERENCES app_user (user_id)
);

CREATE INDEX idx_transfer_enrollment_effective
    ON class_transfer_history (enrollment_id, effective_at);

CREATE TABLE audit_log (
    audit_log_id BIGINT NOT NULL AUTO_INCREMENT,
    actor_user_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    before_data JSON NULL,
    after_data JSON NULL,
    request_id VARCHAR(100) NULL,
    ip_address VARCHAR(45) NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (audit_log_id),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES app_user (user_id)
);

CREATE INDEX idx_audit_entity_time ON audit_log (entity_type, entity_id, occurred_at);
