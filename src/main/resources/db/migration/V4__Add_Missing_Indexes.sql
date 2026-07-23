-- Flyway Migration V4: Add Missing Indexes to align with JPA Entity Metadata (Optimized & Justified)

-- 1. Table: incidents
CREATE INDEX idx_incidents_created_at ON incidents (created_at);

-- 2. Table: incident_comments
CREATE INDEX idx_incident_comments_incident_id ON incident_comments (incident_id);

-- 3. Table: incident_timelines
CREATE INDEX idx_incident_timelines_incident_id ON incident_timelines (incident_id);

-- 4. Table: notifications
CREATE INDEX idx_notifications_created_at ON notifications (created_at);
CREATE INDEX idx_notifications_user_read ON notifications (user_id, read_status);
CREATE INDEX idx_notifications_status_created ON notifications (status, created_at);
CREATE INDEX idx_notifications_status_next_retry ON notifications (status, next_retry_at);

-- 5. Table: notification_outbox
CREATE INDEX idx_outbox_event_id ON notification_outbox (event_id);

-- 6. Table: notification_rate_limits
CREATE INDEX idx_rate_limit_composite ON notification_rate_limits (user_id, notification_type, channel, window_type);

-- 7. Table: user_notification_preferences
CREATE INDEX idx_pref_user_id ON user_notification_preferences (user_id);

-- 8. Table: order_items
CREATE INDEX idx_order_items_order_id ON order_items (order_id);

-- 9. Table: payments
CREATE INDEX idx_payments_order_id ON payments (order_id);

-- 10. Table: payment_transactions
CREATE INDEX idx_transactions_payment_id ON payment_transactions (payment_id);
CREATE INDEX idx_transactions_gateway_ref ON payment_transactions (gateway_reference_id);

-- 11. Table: refunds
CREATE INDEX idx_refunds_payment_id ON refunds (payment_id);

-- 12. Table: products
CREATE INDEX idx_products_rating ON products (rating);
CREATE INDEX idx_products_stock_qty ON products (stock_quantity);

-- 13. Table: api_keys
CREATE INDEX idx_api_key_rotation_hash ON api_keys (rotation_key_hash);

-- 14. Table: idempotency_keys
CREATE INDEX idx_idempotency_key_expiry ON idempotency_keys (expires_at);

-- 15. Table: audit_logs
CREATE INDEX idx_audit_resource ON audit_logs (resource_type, resource_id);
CREATE INDEX idx_audit_correlation ON audit_logs (correlation_id);

-- 16. Table: webhook_deliveries
CREATE INDEX idx_webhook_deliveries_correlation ON webhook_deliveries (correlation_id);
CREATE INDEX idx_webhook_deliveries_aggregate ON webhook_deliveries (aggregate_type, aggregate_id);
