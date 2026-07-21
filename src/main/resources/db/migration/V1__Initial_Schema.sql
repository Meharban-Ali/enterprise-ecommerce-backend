-- V1__Initial_Schema.sql
-- Unified Database Schema for H2 (testing) and MySQL (production)

-- 1. categories
CREATE TABLE categories (
    id bigint NOT NULL AUTO_INCREMENT,
    name varchar(100) NOT NULL UNIQUE,
    description varchar(500),
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 2. products
CREATE TABLE products (
    id bigint NOT NULL AUTO_INCREMENT,
    name varchar(255) NOT NULL,
    price decimal(10,2) NOT NULL,
    rating decimal(2,1) NOT NULL,
    stock_quantity integer NOT NULL,
    category_id bigint,
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 3. users
CREATE TABLE users (
    id bigint NOT NULL AUTO_INCREMENT,
    username varchar(50) NOT NULL,
    email varchar(100) NOT NULL UNIQUE,
    password varchar(255) NOT NULL,
    role varchar(20) NOT NULL,
    account_enabled boolean NOT NULL,
    account_non_locked boolean NOT NULL,
    security_question varchar(255),
    security_answer varchar(255),
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 4. refresh_tokens
CREATE TABLE refresh_tokens (
    id bigint NOT NULL AUTO_INCREMENT,
    token varchar(255) NOT NULL UNIQUE,
    expiry_date datetime(6) NOT NULL,
    user_id bigint NOT NULL,
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 5. carts
CREATE TABLE carts (
    id bigint NOT NULL AUTO_INCREMENT,
    user_id bigint NOT NULL UNIQUE,
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 6. cart_items
CREATE TABLE cart_items (
    id bigint NOT NULL AUTO_INCREMENT,
    cart_id bigint NOT NULL,
    product_id bigint NOT NULL,
    quantity integer NOT NULL,
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 7. wishlists
CREATE TABLE wishlists (
    id bigint NOT NULL AUTO_INCREMENT,
    user_id bigint NOT NULL UNIQUE,
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 8. wishlist_products
CREATE TABLE wishlist_products (
    wishlist_id bigint NOT NULL,
    product_id bigint NOT NULL,
    PRIMARY KEY (wishlist_id, product_id)
);

-- 9. orders
CREATE TABLE orders (
    id bigint NOT NULL AUTO_INCREMENT,
    user_id bigint NOT NULL,
    order_date datetime(6) NOT NULL,
    total_amount decimal(12,2) NOT NULL,
    status varchar(20) NOT NULL,
    shipping_address varchar(500) NOT NULL,
    notes varchar(1000),
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 10. order_items
CREATE TABLE order_items (
    id bigint NOT NULL AUTO_INCREMENT,
    order_id bigint NOT NULL,
    product_id bigint NOT NULL,
    product_name varchar(255) NOT NULL,
    unit_price decimal(10,2) NOT NULL,
    quantity integer not null,
    subtotal decimal(12,2) NOT NULL,
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 11. payments
CREATE TABLE payments (
    id bigint NOT NULL AUTO_INCREMENT,
    order_id bigint NOT NULL UNIQUE,
    payment_method varchar(30) NOT NULL,
    payment_gateway varchar(30) NOT NULL,
    amount decimal(12,2) NOT NULL,
    currency varchar(10) NOT NULL,
    status varchar(30) NOT NULL,
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 12. payment_transactions
CREATE TABLE payment_transactions (
    id bigint NOT NULL AUTO_INCREMENT,
    payment_id bigint NOT NULL,
    type varchar(30) NOT NULL,
    amount decimal(12,2) NOT NULL,
    idempotency_key varchar(255),
    gateway_reference_id varchar(255),
    gateway_response varchar(4000),
    failure_reason varchar(1000),
    webhook_id varchar(255),
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 13. refunds
CREATE TABLE refunds (
    id bigint NOT NULL AUTO_INCREMENT,
    payment_id bigint NOT NULL,
    amount decimal(12,2) NOT NULL,
    status varchar(30) NOT NULL,
    gateway_reference_id varchar(255),
    reason varchar(1000),
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 14. notification_outbox
CREATE TABLE notification_outbox (
    id bigint NOT NULL AUTO_INCREMENT,
    event_id varchar(36) NOT NULL UNIQUE,
    event_type varchar(255) NOT NULL,
    aggregate_type varchar(255),
    aggregate_id varchar(255),
    payload TEXT NOT NULL,
    status varchar(30) NOT NULL,
    retry_count integer NOT NULL,
    processed_at datetime(6),
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 15. notification_templates
CREATE TABLE notification_templates (
    id bigint NOT NULL AUTO_INCREMENT,
    template_code varchar(50) NOT NULL,
    template_name varchar(100) NOT NULL,
    subject varchar(255) NOT NULL,
    html_template TEXT,
    text_template TEXT,
    notification_type varchar(30) NOT NULL,
    notification_channel varchar(30) NOT NULL,
    active boolean NOT NULL,
    version integer NOT NULL,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id),
    UNIQUE (template_code, version)
);

-- 16. notifications
CREATE TABLE notifications (
    id bigint NOT NULL AUTO_INCREMENT,
    user_id bigint NOT NULL,
    title varchar(255) NOT NULL,
    message TEXT NOT NULL,
    type varchar(50) NOT NULL,
    channel varchar(50) NOT NULL,
    priority varchar(20) NOT NULL,
    status varchar(30) NOT NULL,
    read_status boolean NOT NULL,
    read_at datetime(6),
    delivered_at datetime(6),
    retry_count integer NOT NULL,
    last_retry_at datetime(6),
    next_retry_at datetime(6),
    resolved_at datetime(6),
    failure_reason varchar(500),
    last_error_stack TEXT,
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 17. notification_rate_limits
CREATE TABLE notification_rate_limits (
    id bigint NOT NULL AUTO_INCREMENT,
    user_id bigint NOT NULL,
    counter integer NOT NULL,
    window_start datetime(6) NOT NULL,
    window_type varchar(20) NOT NULL,
    channel varchar(30) NOT NULL,
    notification_type varchar(30) NOT NULL,
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 18. user_notification_preferences
CREATE TABLE user_notification_preferences (
    id bigint NOT NULL AUTO_INCREMENT,
    user_id bigint NOT NULL UNIQUE,
    email_enabled boolean NOT NULL,
    sms_enabled boolean NOT NULL,
    push_enabled boolean NOT NULL,
    in_app_enabled boolean NOT NULL,
    marketing_enabled boolean NOT NULL,
    security_mandatory boolean NOT NULL,
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 19. user_sessions
CREATE TABLE user_sessions (
    id bigint NOT NULL AUTO_INCREMENT,
    user_id bigint NOT NULL UNIQUE,
    username varchar(50) NOT NULL,
    email varchar(100) NOT NULL UNIQUE,
    login_time datetime(6),
    logout_time datetime(6),
    last_activity datetime(6),
    status varchar(20) NOT NULL,
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 20. webhook_endpoints
CREATE TABLE webhook_endpoints (
    id bigint NOT NULL AUTO_INCREMENT,
    name varchar(255) NOT NULL,
    target_url varchar(1000) NOT NULL,
    secret_key varchar(255) NOT NULL,
    enabled boolean NOT NULL,
    webhook_version varchar(20) NOT NULL,
    circuit_state varchar(30) NOT NULL,
    consecutive_failures integer NOT NULL,
    last_failure_time datetime(6),
    retry_enabled boolean NOT NULL,
    max_retry_count integer NOT NULL,
    timeout_ms integer NOT NULL,
    batch_enabled boolean NOT NULL,
    batch_size integer,
    batch_interval_seconds integer,
    compression_enabled boolean NOT NULL,
    requests_per_minute integer,
    requests_per_hour integer,
    filter_severity varchar(30),
    filter_priority varchar(30),
    filter_channel varchar(30),
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 21. webhook_endpoint_events
CREATE TABLE webhook_endpoint_events (
    endpoint_id bigint NOT NULL,
    event_type varchar(255) NOT NULL
);

-- 22. webhook_deliveries
CREATE TABLE webhook_deliveries (
    id bigint NOT NULL AUTO_INCREMENT,
    webhook_endpoint_id bigint NOT NULL,
    idempotency_key varchar(100) NOT NULL UNIQUE,
    event_type varchar(50) NOT NULL,
    aggregate_type varchar(100),
    aggregate_id varchar(100),
    payload TEXT NOT NULL,
    delivery_status varchar(30) NOT NULL,
    response_status integer,
    response_body TEXT,
    request_headers TEXT,
    execution_time_ms bigint,
    retry_count integer NOT NULL,
    failure_reason varchar(500),
    correlation_id varchar(100),
    delivered_at datetime(6),
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 23. alert_rules
CREATE TABLE alert_rules (
    id bigint NOT NULL AUTO_INCREMENT,
    rule_code varchar(100) NOT NULL UNIQUE,
    rule_name varchar(255) NOT NULL,
    severity varchar(50) NOT NULL,
    source varchar(50) NOT NULL,
    threshold double precision NOT NULL,
    evaluation_interval_seconds integer NOT NULL,
    cooldown_seconds integer NOT NULL,
    enabled boolean NOT NULL,
    notification_enabled boolean NOT NULL,
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 24. incidents
CREATE TABLE incidents (
    id bigint NOT NULL AUTO_INCREMENT,
    incident_number varchar(50) NOT NULL UNIQUE,
    title varchar(255) NOT NULL,
    description TEXT NOT NULL,
    severity varchar(30) NOT NULL,
    status varchar(30) NOT NULL,
    source varchar(50) NOT NULL,
    escalation_level varchar(30) NOT NULL,
    occurrence_count integer NOT NULL,
    first_occurred_at datetime(6) NOT NULL,
    last_occurred_at datetime(6) NOT NULL,
    acknowledged_at datetime(6),
    acknowledged_by varchar(100),
    resolved_at datetime(6),
    resolved_by varchar(100),
    closed_by varchar(100),
    alert_rule_id bigint NOT NULL,
    sla_deadline datetime(6),
    acknowledgement_deadline datetime(6),
    sla_breached boolean NOT NULL,
    resolved_within_sla boolean,
    resolution_category varchar(50),
    resolution_summary TEXT,
    root_cause TEXT,
    correlation_id varchar(100),
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 25. incident_comments
CREATE TABLE incident_comments (
    id bigint NOT NULL AUTO_INCREMENT,
    incident_id bigint NOT NULL,
    comment TEXT NOT NULL,
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 26. incident_timelines
CREATE TABLE incident_timelines (
    id bigint NOT NULL AUTO_INCREMENT,
    incident_id bigint NOT NULL,
    previous_status varchar(30),
    new_status varchar(30) NOT NULL,
    previous_severity varchar(30),
    new_severity varchar(30) NOT NULL,
    action_performed_by varchar(100) NOT NULL,
    action_source varchar(50) NOT NULL,
    remarks TEXT,
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 27. api_keys
CREATE TABLE api_keys (
    id bigint NOT NULL AUTO_INCREMENT,
    name varchar(255) NOT NULL UNIQUE,
    key_hash varchar(255) NOT NULL UNIQUE,
    enabled boolean NOT NULL,
    revoked boolean NOT NULL,
    expires_at datetime(6),
    rotation_key_hash varchar(255),
    rotation_expires_at datetime(6),
    total_requests bigint NOT NULL,
    failed_requests bigint NOT NULL,
    requests_per_hour bigint NOT NULL,
    requests_per_day bigint NOT NULL,
    success_rate double precision NOT NULL,
    error_rate double precision NOT NULL,
    average_latency_ms double precision NOT NULL,
    rate_limit_violations bigint NOT NULL,
    failed_authentication_count integer NOT NULL,
    last_used_time datetime(6),
    last_successful_authentication datetime(6),
    lock_until datetime(6),
    last_ip_address varchar(255),
    peak_usage_hour integer NOT NULL,
    top_endpoints_json varchar(2048),
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 28. api_key_permissions
CREATE TABLE api_key_permissions (
    api_key_id bigint NOT NULL,
    permission varchar(255) NOT NULL
);

-- 29. audit_logs
CREATE TABLE audit_logs (
    id bigint NOT NULL AUTO_INCREMENT,
    event_id varchar(50) NOT NULL UNIQUE,
    correlation_id varchar(50) NOT NULL,
    request_id varchar(50),
    session_id varchar(50),
    user_id bigint,
    email varchar(100),
    role_name varchar(50),
    ip_address varchar(50),
    user_agent varchar(255),
    client_type varchar(20),
    authentication_method varchar(20),
    actor_type varchar(20),
    action_type varchar(50) NOT NULL,
    resource_type varchar(30) NOT NULL,
    resource_id varchar(50),
    status varchar(30) NOT NULL,
    http_method varchar(10),
    request_uri varchar(255),
    http_status integer,
    execution_time_ms bigint,
    description TEXT,
    entity_version integer,
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 30. backup_metadata
CREATE TABLE backup_metadata (
    id bigint NOT NULL AUTO_INCREMENT,
    backup_type varchar(255) NOT NULL,
    location varchar(255) NOT NULL,
    size_bytes bigint,
    checksum varchar(255) NOT NULL,
    status varchar(255) NOT NULL,
    encryption_status varchar(255),
    compression_status varchar(255),
    verification_status varchar(255),
    retention_type varchar(255),
    duration_ms bigint,
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 31. restore_history
CREATE TABLE restore_history (
    id bigint NOT NULL AUTO_INCREMENT,
    timestamp datetime(6) NOT NULL,
    restore_type varchar(255) NOT NULL,
    status varchar(255) NOT NULL,
    operator varchar(255) NOT NULL,
    verification_result varchar(255),
    correlation_id varchar(255),
    dry_run boolean NOT NULL,
    duration_ms bigint,
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 32. configuration_snapshots
CREATE TABLE configuration_snapshots (
    id bigint NOT NULL AUTO_INCREMENT,
    timestamp datetime(6) NOT NULL,
    changed_by varchar(255) NOT NULL,
    checksum varchar(255) NOT NULL,
    correlation_id varchar(255) NOT NULL,
    version integer NOT NULL,
    PRIMARY KEY (id)
);

-- 33. idempotency_keys
CREATE TABLE idempotency_keys (
    id bigint NOT NULL AUTO_INCREMENT,
    key_value varchar(255) NOT NULL UNIQUE,
    status varchar(255) NOT NULL,
    response_status integer NOT NULL,
    response_body TEXT,
    response_headers TEXT,
    request_fingerprint varchar(255),
    expires_at datetime(6) NOT NULL,
    version integer,
    created_at datetime(6),
    updated_at datetime(6),
    created_by varchar(255),
    updated_by varchar(255),
    PRIMARY KEY (id)
);

-- 34. shedlock
CREATE TABLE shedlock (
    name varchar(64) NOT NULL,
    lock_until timestamp(3) NOT NULL,
    locked_at timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_by varchar(255) NOT NULL,
    PRIMARY KEY (name)
);

-- Indexes for performance
CREATE INDEX idx_products_name ON products (name);
CREATE INDEX idx_products_price ON products (name);
CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_cart_items_cart_id ON cart_items (cart_id);
CREATE INDEX idx_notifications_user_id ON notifications (user_id);
CREATE INDEX idx_notifications_status ON notifications (status);
CREATE INDEX idx_outbox_status_created ON notification_outbox (status, created_at);
CREATE INDEX idx_audit_user ON audit_logs (user_id);
CREATE INDEX idx_audit_action ON audit_logs (action_type);
CREATE INDEX idx_audit_created ON audit_logs (created_at);
CREATE INDEX idx_webhook_deliveries_status ON webhook_deliveries (delivery_status);
CREATE INDEX idx_webhook_deliveries_idempotency ON webhook_deliveries (idempotency_key);
CREATE INDEX idx_api_key_hash ON api_keys (key_hash);
CREATE INDEX idx_incidents_status ON incidents (status);

-- Foreign Key Constraints
ALTER TABLE products ADD CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id);
ALTER TABLE refresh_tokens ADD CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE carts ADD CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE cart_items ADD CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id);
ALTER TABLE cart_items ADD CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products (id);
ALTER TABLE wishlists ADD CONSTRAINT fk_wishlists_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE wishlist_products ADD CONSTRAINT fk_wp_wishlist FOREIGN KEY (wishlist_id) REFERENCES wishlists (id);
ALTER TABLE wishlist_products ADD CONSTRAINT fk_wp_product FOREIGN KEY (product_id) REFERENCES products (id);
ALTER TABLE orders ADD CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id);
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id);
ALTER TABLE payments ADD CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id);
ALTER TABLE payment_transactions ADD CONSTRAINT fk_pt_payment FOREIGN KEY (payment_id) REFERENCES payments (id);
ALTER TABLE refunds ADD CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments (id);
ALTER TABLE notifications ADD CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE user_notification_preferences ADD CONSTRAINT fk_pref_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE user_sessions ADD CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE webhook_deliveries ADD CONSTRAINT fk_wd_endpoint FOREIGN KEY (webhook_endpoint_id) REFERENCES webhook_endpoints (id);
ALTER TABLE webhook_endpoint_events ADD CONSTRAINT fk_wee_endpoint FOREIGN KEY (endpoint_id) REFERENCES webhook_endpoints (id);
ALTER TABLE incident_comments ADD CONSTRAINT fk_ic_incident FOREIGN KEY (incident_id) REFERENCES incidents (id);
ALTER TABLE incident_timelines ADD CONSTRAINT fk_it_incident FOREIGN KEY (incident_id) REFERENCES incidents (id);
ALTER TABLE incidents ADD CONSTRAINT fk_incidents_rule FOREIGN KEY (alert_rule_id) REFERENCES alert_rules (id);
ALTER TABLE api_key_permissions ADD CONSTRAINT fk_akp_key FOREIGN KEY (api_key_id) REFERENCES api_keys (id);
