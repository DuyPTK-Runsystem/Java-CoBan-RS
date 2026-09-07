-- V15: Create semester completeness notification table for CR-SEM-001
CREATE TABLE semester_completeness_notification (
    id BIGINT NOT NULL AUTO_INCREMENT,
    semester_id BIGINT NOT NULL,
    report_id BIGINT NULL,
    checkpoint_code VARCHAR(20) NOT NULL,
    recipient_email VARCHAR(150) NOT NULL,
    recipient_role VARCHAR(50) NOT NULL,
    recipient_teacher_id BIGINT NULL,
    notification_channel VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    subject VARCHAR(255) NOT NULL,
    body_content TEXT NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    sent_at TIMESTAMP NULL,
    error_message TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_scn_semester FOREIGN KEY (semester_id) REFERENCES semester (semester_id),
    CONSTRAINT fk_scn_report FOREIGN KEY (report_id) REFERENCES semester_lock_report (report_id),
    CONSTRAINT fk_scn_teacher FOREIGN KEY (recipient_teacher_id) REFERENCES teacher (teacher_id),
    CONSTRAINT ck_scn_channel CHECK (notification_channel IN ('EMAIL', 'IN_APP')),
    CONSTRAINT ck_scn_status CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    CONSTRAINT uk_sem_notif_chk_recip UNIQUE (semester_id, checkpoint_code, recipient_email, notification_channel)
);

CREATE INDEX idx_scn_sem_chk ON semester_completeness_notification (semester_id, checkpoint_code);
CREATE INDEX idx_scn_status ON semester_completeness_notification (status);
