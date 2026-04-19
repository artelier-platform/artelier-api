-- ============================================================
-- V4: Product Images
-- Matches: ProductImage.java
-- ============================================================

CREATE TABLE product_images (
                                id            UUID     PRIMARY KEY DEFAULT gen_random_uuid(),
                                product_id    UUID     NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                                url           TEXT     NOT NULL,
                                cloudinary_id TEXT,
                                is_primary    BOOLEAN  NOT NULL DEFAULT FALSE,
                                sort_order    INTEGER  NOT NULL DEFAULT 0
);

CREATE INDEX idx_product_images_product_id ON product_images(product_id);

CREATE UNIQUE INDEX idx_one_primary_per_product
    ON product_images(product_id)
    WHERE is_primary = TRUE;