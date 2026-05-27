-- =========================
-- ORDERS TABLE
-- =========================
CREATE TABLE orders (
                        id UUID PRIMARY KEY,

                        user_id UUID NOT NULL,

                        status VARCHAR(30) NOT NULL,

                        subtotal DECIMAL(10,2) NOT NULL,
                        total DECIMAL(10,2) NOT NULL,

                        shipping_address VARCHAR(255) NOT NULL,
                        notes TEXT,

                        created_at TIMESTAMP NOT NULL,

                        CONSTRAINT fk_orders_user
                            FOREIGN KEY (user_id)
                                REFERENCES users(id)
                                ON DELETE CASCADE
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);



-- =========================
-- ORDER ITEMS TABLE
-- =========================
CREATE TABLE order_items (
                             id UUID PRIMARY KEY,

                             order_id UUID NOT NULL,
                             product_id UUID NOT NULL,

                             quantity INT NOT NULL,

                             unit_price DECIMAL(10,2) NOT NULL,

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

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);