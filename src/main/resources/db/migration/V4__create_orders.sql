-- ============================================================
-- V6: Orders
-- ============================================================

CREATE TABLE orders (
                        id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
                        user_id          UUID          NOT NULL,
                        status           VARCHAR(30)   NOT NULL,
                        customer_email   VARCHAR(255)  NOT NULL,
                        contact_phone    VARCHAR(20),
                        subtotal         NUMERIC(10,2) NOT NULL CHECK (subtotal >= 0),
                        total            NUMERIC(10,2) NOT NULL CHECK (total >= 0),
                        shipping_address VARCHAR(255)  NOT NULL,
                        notes            TEXT,
                        created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

                        CONSTRAINT fk_orders_user
                            FOREIGN KEY (user_id)
                                REFERENCES users(id)
                                ON DELETE RESTRICT
);

CREATE INDEX idx_orders_user_id
    ON orders(user_id);

CREATE INDEX idx_orders_status_created_at
    ON orders(status, created_at);


-- ============================================================
-- Order Items
-- ============================================================

CREATE TABLE order_items (
                             id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
                             order_id     UUID          NOT NULL,
                             product_id   UUID          NOT NULL,
                             quantity     INTEGER       NOT NULL CHECK (quantity > 0),
                             unit_price   NUMERIC(10,2) NOT NULL CHECK (unit_price > 0),
                             custom_notes TEXT,

                             CONSTRAINT fk_order_items_order
                                 FOREIGN KEY (order_id)
                                     REFERENCES orders(id)
                                     ON DELETE CASCADE,

                             CONSTRAINT fk_order_items_product
                                 FOREIGN KEY (product_id)
                                     REFERENCES products(id)
                                     ON DELETE RESTRICT
);

CREATE INDEX idx_order_items_order_id
    ON order_items(order_id);

CREATE INDEX idx_order_items_product_id
    ON order_items(product_id);