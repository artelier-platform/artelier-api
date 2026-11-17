-- ============================================================
-- V5: Reviews
-- ============================================================

CREATE TABLE reviews (
                         id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                         user_id        UUID         NOT NULL,
                         order_items_id UUID         NOT NULL,
                         product_id     UUID         NOT NULL,
                         rating         INTEGER      NOT NULL,
                         comment        VARCHAR(2000),
                         created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                         updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

                         CONSTRAINT fk_reviews_user
                             FOREIGN KEY (user_id)
                                 REFERENCES users(id)
                                 ON DELETE RESTRICT,

                         CONSTRAINT fk_reviews_order_item
                             FOREIGN KEY (order_items_id)
                                 REFERENCES order_items(id)
                                 ON DELETE RESTRICT,

                         CONSTRAINT fk_reviews_product
                             FOREIGN KEY (product_id)
                                 REFERENCES products(id)
                                 ON DELETE RESTRICT,

                         CONSTRAINT chk_reviews_rating
                             CHECK (rating BETWEEN 1 AND 5),

                         CONSTRAINT uk_reviews_order_item
                             UNIQUE (order_items_id)
);

CREATE INDEX idx_reviews_user_id
    ON reviews(user_id);

CREATE INDEX idx_reviews_product_id
    ON reviews(product_id);

CREATE INDEX idx_reviews_created_at
    ON reviews(created_at DESC);