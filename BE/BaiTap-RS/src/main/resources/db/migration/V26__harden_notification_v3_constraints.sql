-- V26: Add the notification scope constraint after immutable V25.
-- V25 owns the fingerprint column and the IN_APP-only channel constraint.

ALTER TABLE notification
    ADD CONSTRAINT ck_notification_school_scope CHECK (school_scope = 'DEFAULT_SCHOOL');
