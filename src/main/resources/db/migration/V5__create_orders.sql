
-- ============================================================
-- V5: Orders
-- ============================================================

CREATE TABLE orders (
                        id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
                        user_id          UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                        status           VARCHAR(30)   NOT NULL,
                        customer_email   VARCHAR(255)  NOT NULL,
                        subtotal         NUMERIC(10,2) NOT NULL,
                        total            NUMERIC(10,2) NOT NULL,
                        shipping_address VARCHAR(255)  NOT NULL,
                        notes            TEXT,
                        created_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status  ON orders(status);


-- ============================================================
-- Order Items
-- ============================================================

CREATE TABLE order_items (
                             id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
                             order_id     UUID          NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                             product_id   UUID          NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
                             quantity     INT           NOT NULL CHECK (quantity > 0),
                             unit_price   NUMERIC(10,2) NOT NULL,
                             custom_notes TEXT
);

CREATE INDEX idx_order_items_order_id   ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
