-- ============================================================
-- V3: Products
-- ============================================================

CREATE TYPE stock_type_enum AS ENUM (
    'UNLIMITED',
    'AVAILABLE',
    'MADE_TO_ORDER'
);

CREATE TABLE products (
                          id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
                          category_id     UUID            NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
                          name            VARCHAR(150)    NOT NULL,
                          slug            VARCHAR(160)    NOT NULL UNIQUE,
                          description     VARCHAR(500),
                          story           TEXT,
                          price           NUMERIC(10,2)   NOT NULL CHECK (price > 0),
                          stock_type      stock_type_enum NOT NULL,
                          stock_quantity  INTEGER,
                          is_custom_order BOOLEAN         NOT NULL DEFAULT FALSE,
                          is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
                          created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                          deleted_at      TIMESTAMPTZ,

                          CONSTRAINT chk_stock_quantity CHECK (
                              (stock_type = 'AVAILABLE' AND stock_quantity IS NOT NULL AND stock_quantity >= 0)
                                  OR (stock_type IN ('UNLIMITED', 'MADE_TO_ORDER') AND stock_quantity IS NULL)
                              )
);

CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_active ON products(is_active)
    WHERE is_active = TRUE AND deleted_at IS NULL;


-- ============================================================
-- Product Images
-- ============================================================

CREATE TABLE product_images (
                                id            UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
                                product_id    UUID    NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                                url           TEXT    NOT NULL,
                                cloudinary_id TEXT,
                                is_primary    BOOLEAN NOT NULL DEFAULT FALSE,
                                sort_order    INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_product_images_product_id
    ON product_images(product_id);

CREATE UNIQUE INDEX idx_one_primary_per_product
    ON product_images(product_id)
    WHERE is_primary = TRUE;