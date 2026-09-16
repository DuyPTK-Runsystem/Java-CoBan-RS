-- Plan 076: Targeted In-app Notification v3 Schema
-- Tables: notification, notification_receipt

CREATE TABLE notification (
    notification_id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    channel VARCHAR(30) NOT NULL DEFAULT 'IN_APP',
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    audience_type VARCHAR(30) NOT NULL,
    target_reference VARCHAR(255) NULL,
    school_scope VARCHAR(100) NOT NULL DEFAULT 'DEFAULT_SCHOOL',
    sender_id BIGINT NOT NULL,
    publish_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    idempotency_key VARCHAR(100) NULL,
    idempotency_fingerprint CHAR(64) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notification_id),
    CONSTRAINT uk_notification_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_notification_channel CHECK (channel IN ('IN_APP')),
    CONSTRAINT ck_notification_status CHECK (status IN ('DRAFT', 'SCHEDULED', 'PUBLISHED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT ck_notification_audience CHECK (audience_type IN ('INDIVIDUAL', 'CLASS', 'SCHOOL')),
    CONSTRAINT fk_notification_sender FOREIGN KEY (sender_id) REFERENCES app_user (user_id)
);

CREATE INDEX idx_notification_scope_status ON notification (school_scope, status);
CREATE INDEX idx_notification_sender ON notification (sender_id);
CREATE INDEX idx_notification_publish_at ON notification (publish_at);

CREATE TABLE notification_receipt (
    receipt_id BIGINT NOT NULL AUTO_INCREMENT,
    notification_id BIGINT NOT NULL,
    recipient_user_id BIGINT NOT NULL,
    access_scope VARCHAR(30) NOT NULL,
    read_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (receipt_id),
    CONSTRAINT uk_receipt_notification_recipient UNIQUE (notification_id, recipient_user_id),
    CONSTRAINT fk_receipt_notification FOREIGN KEY (notification_id) REFERENCES notification (notification_id),
    CONSTRAINT fk_receipt_recipient FOREIGN KEY (recipient_user_id) REFERENCES app_user (user_id)
);

CREATE INDEX idx_receipt_recipient_read ON notification_receipt (recipient_user_id, read_at);
CREATE INDEX idx_receipt_notification ON notification_receipt (notification_id);
