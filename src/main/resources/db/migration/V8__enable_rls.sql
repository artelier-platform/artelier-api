-- ============================================================
-- V8__enable_rls.sql
-- ============================================================

-- USERS
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- REFRESH TOKENS
ALTER TABLE refresh_tokens ENABLE ROW LEVEL SECURITY;

-- CATEGORIES
ALTER TABLE categories ENABLE ROW LEVEL SECURITY;

-- PRODUCTS
ALTER TABLE products ENABLE ROW LEVEL SECURITY;

-- PRODUCT IMAGES
ALTER TABLE product_images ENABLE ROW LEVEL SECURITY;

-- ORDERS
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;

-- ORDER ITEMS
ALTER TABLE order_items ENABLE ROW LEVEL SECURITY;

-- PAYMENTS
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;

-- ============================================================
-- Policies
-- ============================================================

CREATE POLICY users_all_access
ON users
FOR ALL
USING (true)
WITH CHECK (true);

CREATE POLICY refresh_tokens_all_access
ON refresh_tokens
FOR ALL
USING (true)
WITH CHECK (true);

CREATE POLICY categories_all_access
ON categories
FOR ALL
USING (true)
WITH CHECK (true);

CREATE POLICY products_all_access
ON products
FOR ALL
USING (true)
WITH CHECK (true);

CREATE POLICY product_images_all_access
ON product_images
FOR ALL
USING (true)
WITH CHECK (true);

CREATE POLICY orders_all_access
ON orders
FOR ALL
USING (true)
WITH CHECK (true);

CREATE POLICY order_items_all_access
ON order_items
FOR ALL
USING (true)
WITH CHECK (true);

CREATE POLICY payments_all_access
ON payments
FOR ALL
USING (true)
WITH CHECK (true);
