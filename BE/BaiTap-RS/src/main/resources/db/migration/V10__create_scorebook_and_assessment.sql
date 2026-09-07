CREATE TABLE scorebook (
    scorebook_id BIGINT NOT NULL AUTO_INCREMENT,
    class_subject_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    published_at TIMESTAMP NULL,
    published_by BIGINT NULL,
    closed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (scorebook_id),
    CONSTRAINT uk_scorebook_class_subject UNIQUE (class_subject_id),
    CONSTRAINT ck_scorebook_status CHECK (status IN ('DRAFT', 'OPEN', 'PUBLISHED', 'CLOSED')),
    CONSTRAINT fk_scorebook_class_subject FOREIGN KEY (class_subject_id)
        REFERENCES class_subject (class_subject_id),
    CONSTRAINT fk_scorebook_published_by FOREIGN KEY (published_by)
        REFERENCES app_user (user_id)
);

CREATE INDEX idx_scorebook_class_subject_status ON scorebook (class_subject_id, status);

CREATE TABLE assessment_column (
    assessment_column_id BIGINT NOT NULL AUTO_INCREMENT,
    scorebook_id BIGINT NOT NULL,
    assessment_type VARCHAR(20) NOT NULL,
    column_no INT NOT NULL,
    column_name VARCHAR(100) NULL,
    weight_factor DECIMAL(5, 2) NOT NULL,
    is_required BOOLEAN NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (assessment_column_id),
    CONSTRAINT uk_assessment_column_position UNIQUE (scorebook_id, assessment_type, column_no),
    CONSTRAINT ck_assessment_column_type CHECK (assessment_type IN ('KTTT', 'KTDK', 'KTCK')),
    CONSTRAINT ck_assessment_column_no CHECK (column_no > 0),
    CONSTRAINT ck_assessment_column_weight CHECK (weight_factor > 0 AND weight_factor <= 100),
    CONSTRAINT ck_assessment_column_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT fk_assessment_column_scorebook FOREIGN KEY (scorebook_id)
        REFERENCES scorebook (scorebook_id)
);

CREATE INDEX idx_assessment_column_scorebook_status
    ON assessment_column (scorebook_id, assessment_type, status);

CREATE TABLE skill_weight_config (
    skill_weight_config_id BIGINT NOT NULL AUTO_INCREMENT,
    scorebook_id BIGINT NOT NULL,
    kttt_weight_percent DECIMAL(5, 2) NOT NULL,
    ktdk_weight_percent DECIMAL(5, 2) NOT NULL,
    ktck_weight_percent DECIMAL(5, 2) NOT NULL,
    configured_by BIGINT NOT NULL,
    configured_at TIMESTAMP NOT NULL,
    locked_by BIGINT NULL,
    locked_at TIMESTAMP NULL,
    PRIMARY KEY (skill_weight_config_id),
    CONSTRAINT uk_skill_weight_scorebook UNIQUE (scorebook_id),
    CONSTRAINT ck_skill_weight_non_negative CHECK (
        kttt_weight_percent >= 0 AND kttt_weight_percent <= 100
        AND ktdk_weight_percent >= 0 AND ktdk_weight_percent <= 100
        AND ktck_weight_percent >= 0 AND ktck_weight_percent <= 100
    ),
    CONSTRAINT ck_skill_weight_total CHECK (
        kttt_weight_percent + ktdk_weight_percent + ktck_weight_percent = 100
    ),
    CONSTRAINT ck_skill_weight_final_not_less CHECK (
        ktck_weight_percent >= kttt_weight_percent
        AND ktck_weight_percent >= ktdk_weight_percent
    ),
    CONSTRAINT fk_skill_weight_scorebook FOREIGN KEY (scorebook_id)
        REFERENCES scorebook (scorebook_id),
    CONSTRAINT fk_skill_weight_configured_by FOREIGN KEY (configured_by)
        REFERENCES app_user (user_id),
    CONSTRAINT fk_skill_weight_locked_by FOREIGN KEY (locked_by)
        REFERENCES app_user (user_id)
);
