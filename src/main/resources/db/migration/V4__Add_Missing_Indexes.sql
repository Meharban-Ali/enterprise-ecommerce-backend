-- Flyway Migration V4: Add Missing Indexes to align with JPA Entity Metadata

-- 1. Table: incidents
CREATE INDEX idx_incidents_severity ON incidents (severity);
CREATE INDEX idx_incidents_source ON incidents (source);
CREATE INDEX idx_incidents_sla_deadline ON incidents (sla_deadline);
CREATE INDEX idx_incidents_created_at ON incidents (created_at);
CREATE INDEX idx_incidents_incident_number ON incidents (incident_number);
CREATE INDEX idx_incidents_escalation_level ON incidents (escalation_level);

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
CREATE INDEX idx_outbox_status_retry ON notification_outbox (status, retry_count);

-- 6. Table: notification_rate_limits
CREATE INDEX idx_rate_limit_user ON notification_rate_limits (user_id);
CREATE INDEX idx_rate_limit_composite ON notification_rate_limits (user_id, notification_type, channel, window_type);

-- 7. Table: notification_templates
CREATE INDEX idx_template_code ON notification_templates (template_code);
CREATE INDEX idx_template_type ON notification_templates (notification_type);
CREATE INDEX idx_template_channel ON notification_templates (notification_channel);
CREATE INDEX idx_template_active ON notification_templates (active);

-- 8. Table: user_notification_preferences
CREATE INDEX idx_pref_user_id ON user_notification_preferences (user_id);

-- 9. Table: orders
CREATE INDEX idx_orders_order_date ON orders (order_date);

-- 10. Table: order_items
CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);

-- 11. Table: payments
CREATE INDEX idx_payments_order_id ON payments (order_id);
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_created_at ON payments (created_at);

-- 12. Table: payment_transactions
CREATE INDEX idx_transactions_payment_id ON payment_transactions (payment_id);
CREATE INDEX idx_transactions_gateway_ref ON payment_transactions (gateway_reference_id);

-- 13. Table: refunds
CREATE INDEX idx_refunds_payment_id ON refunds (payment_id);
CREATE INDEX idx_refunds_status ON refunds (status);

-- 14. Table: products
CREATE INDEX idx_products_rating ON products (rating);
CREATE INDEX idx_products_stock_qty ON products (stock_quantity);

-- 15. Table: api_keys
CREATE INDEX idx_api_key_rotation_hash ON api_keys (rotation_key_hash);

-- 16. Table: idempotency_keys
CREATE INDEX idx_idempotency_key_lookup ON idempotency_keys (key_value);
CREATE INDEX idx_idempotency_key_expiry ON idempotency_keys (expires_at);

-- 17. Table: user_sessions
CREATE UNIQUE INDEX idx_user_sessions_user_id ON user_sessions (user_id);
CREATE UNIQUE INDEX idx_user_sessions_email ON user_sessions (email);

-- 18. Table: webhook_deliveries
CREATE INDEX idx_webhook_deliveries_event_type ON webhook_deliveries (event_type);
CREATE INDEX idx_webhook_deliveries_correlation ON webhook_deliveries (correlation_id);
CREATE INDEX idx_webhook_deliveries_created_at ON webhook_deliveries (created_at);
CREATE INDEX idx_webhook_deliveries_aggregate ON webhook_deliveries (aggregate_type, aggregate_id);

-- 19. Table: webhook_endpoints
CREATE INDEX idx_webhook_endpoints_enabled ON webhook_endpoints (enabled);
CREATE INDEX idx_webhook_endpoints_target_url ON webhook_endpoints (target_url);
