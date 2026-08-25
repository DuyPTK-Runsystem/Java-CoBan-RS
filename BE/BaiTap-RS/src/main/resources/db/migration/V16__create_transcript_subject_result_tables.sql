-- Migration V16: Create transcript subject result tables and enrich transcript schemas

ALTER TABLE student_annual_transcript
    ADD COLUMN regular_dtbcn DECIMAL(3,1) NULL;

ALTER TABLE student_annual_transcript
    ADD COLUMN final_dtbcn DECIMAL(3,1) NULL;

ALTER TABLE student_annual_transcript
    ADD COLUMN result_source VARCHAR(20) NULL;

ALTER TABLE student_annual_transcript
    ADD COLUMN last_calculation_task_id BIGINT NULL;

ALTER TABLE student_annual_transcript
    ADD CONSTRAINT ck_annual_transcript_result_source
        CHECK (result_source IN ('REGULAR', 'RETAKE') OR result_source IS NULL);

ALTER TABLE student_annual_transcript
    ADD CONSTRAINT ck_annual_transcript_regular_dtbcn
        CHECK (regular_dtbcn IS NULL OR (regular_dtbcn >= 0.0 AND regular_dtbcn <= 10.0));

ALTER TABLE student_annual_transcript
    ADD CONSTRAINT ck_annual_transcript_final_dtbcn
        CHECK (final_dtbcn IS NULL OR (final_dtbcn >= 0.0 AND final_dtbcn <= 10.0));

ALTER TABLE student_annual_transcript
    ADD CONSTRAINT fk_annual_transcript_calc_task
        FOREIGN KEY (last_calculation_task_id) REFERENCES calculation_task (task_id);

ALTER TABLE student_term_transcript
    ADD COLUMN dtbhk DECIMAL(3,1) NULL;

ALTER TABLE student_term_transcript
    ADD CONSTRAINT ck_term_transcript_dtbhk
        CHECK (dtbhk IS NULL OR (dtbhk >= 0.0 AND dtbhk <= 10.0));

CREATE TABLE student_subject_term_result (
    term_result_id BIGINT NOT NULL AUTO_INCREMENT,
    term_transcript_id BIGINT NOT NULL,
    class_subject_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    subject_type VARCHAR(20) NOT NULL,
    dtbmh DECIMAL(3,1) NULL,
    skill_score DECIMAL(3,1) NULL,
    calculated_version BIGINT NULL,
    calculated_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (term_result_id),
    CONSTRAINT uk_subject_term_result UNIQUE (term_transcript_id, subject_id),
    CONSTRAINT ck_subject_term_result_type
        CHECK (subject_type IN ('NORMAL', 'SKILL', 'ACADEMIC')),
    CONSTRAINT ck_subject_term_result_dtbmh
        CHECK (dtbmh IS NULL OR (dtbmh >= 0.0 AND dtbmh <= 10.0)),
    CONSTRAINT ck_subject_term_result_skill
        CHECK (skill_score IS NULL OR (skill_score >= 0.0 AND skill_score <= 10.0)),
    CONSTRAINT fk_subject_term_result_transcript
        FOREIGN KEY (term_transcript_id) REFERENCES student_term_transcript (term_transcript_id),
    CONSTRAINT fk_subject_term_result_class_subject
        FOREIGN KEY (class_subject_id) REFERENCES class_subject (class_subject_id),
    CONSTRAINT fk_subject_term_result_subject
        FOREIGN KEY (subject_id) REFERENCES subject (subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_subject_term_result_transcript
    ON student_subject_term_result (term_transcript_id);
CREATE INDEX idx_subject_term_result_class_subject
    ON student_subject_term_result (class_subject_id);

CREATE TABLE student_subject_annual_result (
    annual_subject_result_id BIGINT NOT NULL AUTO_INCREMENT,
    annual_transcript_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    hk1_term_result_id BIGINT NULL,
    hk2_term_result_id BIGINT NULL,
    retake_id BIGINT NULL,
    subject_type VARCHAR(20) NOT NULL,
    regular_dtbmh_cn DECIMAL(3,1) NULL,
    official_dtbmh_cn DECIMAL(3,1) NULL,
    calculation_source VARCHAR(20) NULL,
    calculated_version BIGINT NULL,
    calculated_at TIMESTAMP NULL,
    note VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (annual_subject_result_id),
    CONSTRAINT uk_subject_annual_result UNIQUE (annual_transcript_id, subject_id),
    CONSTRAINT ck_subject_annual_result_type
        CHECK (subject_type IN ('NORMAL', 'SKILL', 'ACADEMIC')),
    CONSTRAINT ck_subject_annual_result_source
        CHECK (calculation_source IN ('REGULAR', 'RETAKE') OR calculation_source IS NULL),
    CONSTRAINT ck_subject_annual_result_regular_dtbmh
        CHECK (regular_dtbmh_cn IS NULL OR (regular_dtbmh_cn >= 0.0 AND regular_dtbmh_cn <= 10.0)),
    CONSTRAINT ck_subject_annual_result_official_dtbmh
        CHECK (official_dtbmh_cn IS NULL OR (official_dtbmh_cn >= 0.0 AND official_dtbmh_cn <= 10.0)),
    CONSTRAINT fk_subject_annual_result_transcript
        FOREIGN KEY (annual_transcript_id) REFERENCES student_annual_transcript (annual_transcript_id),
    CONSTRAINT fk_subject_annual_result_subject
        FOREIGN KEY (subject_id) REFERENCES subject (subject_id),
    CONSTRAINT fk_subject_annual_result_hk1
        FOREIGN KEY (hk1_term_result_id) REFERENCES student_subject_term_result (term_result_id),
    CONSTRAINT fk_subject_annual_result_hk2
        FOREIGN KEY (hk2_term_result_id) REFERENCES student_subject_term_result (term_result_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_subject_annual_result_transcript
    ON student_subject_annual_result (annual_transcript_id);
CREATE INDEX idx_subject_annual_result_subject
    ON student_subject_annual_result (subject_id);
