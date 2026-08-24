-- Migration V11: Create student_score table

CREATE TABLE student_score (
    score_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    assessment_column_id BIGINT UNSIGNED NOT NULL,
    student_id BIGINT UNSIGNED NOT NULL,
    score_status VARCHAR(20) NOT NULL,
    score_value DECIMAL(3,1) NULL,
    note VARCHAR(500) NULL,
    entered_by BIGINT UNSIGNED NOT NULL,
    entered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT UNSIGNED NULL,
    updated_at TIMESTAMP NULL,
    version BIGINT UNSIGNED NOT NULL DEFAULT 1,
    PRIMARY KEY (score_id),
    CONSTRAINT uk_student_score_column_student UNIQUE (assessment_column_id, student_id),
    CONSTRAINT ck_student_score_status CHECK (score_status IN ('SCORED', 'ABSENT', 'EXEMPTED', 'CANCELLED')),
    CONSTRAINT ck_student_score_value CHECK (
        (score_status = 'SCORED' AND score_value IS NOT NULL AND score_value >= 0.0 AND score_value <= 10.0)
        OR
        (score_status <> 'SCORED' AND score_value IS NULL)
    ),
    CONSTRAINT fk_student_score_column FOREIGN KEY (assessment_column_id) REFERENCES assessment_column(assessment_column_id),
    CONSTRAINT fk_student_score_student FOREIGN KEY (student_id) REFERENCES student(student_id),
    CONSTRAINT fk_student_score_entered_by FOREIGN KEY (entered_by) REFERENCES app_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_student_score_column_status ON student_score (assessment_column_id, score_status);
CREATE INDEX idx_student_score_student ON student_score (student_id);
