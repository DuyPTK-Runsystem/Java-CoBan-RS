-- Migration V17: Create retake exam table and constraints

CREATE TABLE retake_exam (
    retake_id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    academic_year_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    pre_retake_score DECIMAL(3,1) NOT NULL,
    retake_score DECIMAL(3,1) NULL,
    exam_date DATE NULL,
    status VARCHAR(20) NOT NULL,
    note VARCHAR(1000) NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (retake_id),
    CONSTRAINT uk_retake_student_year_subject UNIQUE (student_id, academic_year_id, subject_id),
    CONSTRAINT ck_retake_exam_status
        CHECK (status IN ('PLANNED', 'SCORED', 'CANCELLED')),
    CONSTRAINT ck_retake_pre_score
        CHECK (pre_retake_score >= 0.0 AND pre_retake_score <= 10.0),
    CONSTRAINT ck_retake_score
        CHECK (retake_score IS NULL OR (retake_score >= 0.0 AND retake_score <= 10.0)),
    CONSTRAINT fk_retake_exam_student
        FOREIGN KEY (student_id) REFERENCES student (student_id),
    CONSTRAINT fk_retake_exam_academic_year
        FOREIGN KEY (academic_year_id) REFERENCES academic_year (academic_year_id),
    CONSTRAINT fk_retake_exam_subject
        FOREIGN KEY (subject_id) REFERENCES subject (subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_retake_exam_student_year
    ON retake_exam (student_id, academic_year_id);
CREATE INDEX idx_retake_exam_status
    ON retake_exam (status);

ALTER TABLE student_subject_annual_result
    ADD CONSTRAINT fk_subject_annual_result_retake
        FOREIGN KEY (retake_id) REFERENCES retake_exam (retake_id);

