-- V27: Enable v3 EMAIL channel and persist per-recipient delivery outcome.
-- Existing IN_APP notifications and receipts remain valid.

ALTER TABLE notification
    DROP CONSTRAINT ck_notification_channel;

ALTER TABLE notification
    ADD CONSTRAINT ck_notification_channel CHECK (channel IN ('IN_APP', 'EMAIL'));

ALTER TABLE notification_receipt
    ADD COLUMN delivery_status VARCHAR(30) NULL,
    ADD COLUMN delivery_error VARCHAR(1000) NULL,
    ADD COLUMN delivered_at TIMESTAMP NULL,
    ADD CONSTRAINT ck_notification_receipt_delivery_status
        CHECK (delivery_status IS NULL OR delivery_status IN ('PENDING', 'SENT', 'FAILED'));
