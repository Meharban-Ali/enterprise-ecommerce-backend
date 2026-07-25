-- V5: Add structured reference fields to notifications table for transaction traceability
ALTER TABLE notifications ADD COLUMN reference_entity_id BIGINT NULL;
ALTER TABLE notifications ADD COLUMN reference_entity_type VARCHAR(50) NULL;
ALTER TABLE notifications ADD COLUMN action_url VARCHAR(500) NULL;

CREATE INDEX idx_notifications_reference ON notifications (reference_entity_type, reference_entity_id);
