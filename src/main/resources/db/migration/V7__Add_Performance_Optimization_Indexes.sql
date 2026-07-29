-- Flyway Migration V7: Add Performance Optimization Indexes
-- Aligns database schema with high-traffic repository WHERE and JOIN queries.

-- 1. Table: password_reset_tokens (foreign key lookup for user token validations)
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);

-- 2. Table: payments (payment expiration scheduler & status query index)
CREATE INDEX idx_payments_status ON payments (status);
