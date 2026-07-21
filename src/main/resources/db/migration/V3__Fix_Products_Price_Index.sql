-- V3: Correct products price index column mapping
DROP INDEX idx_products_price ON products;
CREATE INDEX idx_products_price ON products (price);
