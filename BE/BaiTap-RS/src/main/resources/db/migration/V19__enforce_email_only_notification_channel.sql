-- V19: Enforce the approved email-only notification contract.
-- Deliberately does not update or delete existing rows. If legacy IN_APP rows
-- exist, this migration fails safely and must be preceded by an approved data
-- remediation plan.
ALTER TABLE semester_completeness_notification
    ADD CONSTRAINT ck_scn_channel_email_only CHECK (notification_channel IN ('EMAIL'));

ALTER TABLE semester_completeness_notification
    DROP CONSTRAINT ck_scn_channel;
