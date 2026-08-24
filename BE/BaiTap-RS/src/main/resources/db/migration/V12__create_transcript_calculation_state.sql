-- Migration V12: Create transcript calculation state tables

CREATE TABLE student_annual_transcript (
    annual_transcript_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    student_id BIGINT UNSIGNED NOT NULL,
    academic_year_id BIGINT UNSIGNED NOT NULL,
    calculation_status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    source_version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    calculated_version BIGINT UNSIGNED NULL,
    calculated_at TIMESTAMP NULL,
    last_error VARCHAR(2000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (annual_transcript_id),
    CONSTRAINT uk_annual_transcript_student_year UNIQUE (student_id, academic_year_id),
    CONSTRAINT ck_annual_transcript_status CHECK (calculation_status IN ('IN_PROGRESS', 'FINISH')),
    CONSTRAINT fk_annual_transcript_student FOREIGN KEY (student_id) REFERENCES student(student_id),
    CONSTRAINT fk_annual_transcript_year FOREIGN KEY (academic_year_id) REFERENCES academic_year(academic_year_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE student_term_transcript (
    term_transcript_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    annual_transcript_id BIGINT UNSIGNED NOT NULL,
    semester_id BIGINT UNSIGNED NOT NULL,
    student_id BIGINT UNSIGNED NOT NULL,
    calculation_status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    source_version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    calculated_version BIGINT UNSIGNED NULL,
    calculated_at TIMESTAMP NULL,
    last_error VARCHAR(2000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (term_transcript_id),
    CONSTRAINT uk_term_transcript_annual_semester UNIQUE (annual_transcript_id, semester_id),
    CONSTRAINT ck_term_transcript_status CHECK (calculation_status IN ('IN_PROGRESS', 'FINISH')),
    CONSTRAINT fk_term_transcript_annual FOREIGN KEY (annual_transcript_id) REFERENCES student_annual_transcript(annual_transcript_id),
    CONSTRAINT fk_term_transcript_semester FOREIGN KEY (semester_id) REFERENCES semester(semester_id),
    CONSTRAINT fk_term_transcript_student FOREIGN KEY (student_id) REFERENCES student(student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE calculation_task (
    task_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    student_id BIGINT UNSIGNED NOT NULL,
    academic_year_id BIGINT UNSIGNED NOT NULL,
    task_type VARCHAR(30) NOT NULL,
    requested_version BIGINT UNSIGNED NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INT UNSIGNED NOT NULL DEFAULT 0,
    max_attempts INT UNSIGNED NOT NULL DEFAULT 3,
    available_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_at TIMESTAMP NULL,
    worker_id VARCHAR(100) NULL,
    last_error VARCHAR(2000) NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    PRIMARY KEY (task_id),
    CONSTRAINT uk_calculation_task_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_calculation_task_type CHECK (task_type IN ('STUDENT_YEAR_RECALC')),
    CONSTRAINT ck_calculation_task_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT fk_calculation_task_student FOREIGN KEY (student_id) REFERENCES student(student_id),
    CONSTRAINT fk_calculation_task_year FOREIGN KEY (academic_year_id) REFERENCES academic_year(academic_year_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_calculation_task_status_available ON calculation_task (status, available_at);
CREATE INDEX idx_calculation_task_student_year ON calculation_task (student_id, academic_year_id);
