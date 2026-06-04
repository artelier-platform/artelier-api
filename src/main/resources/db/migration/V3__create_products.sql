
-- ============================================================
-- V3: Products
-- ============================================================

CREATE TYPE stock_type_enum AS ENUM ('AVAILABLE', 'MADE_TO_ORDER');

CREATE TABLE products (
                          id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
                          category_id     UUID            NOT NULL REFERENCES categories(id),
                          name            VARCHAR(150)    NOT NULL,
                          slug            VARCHAR(160)    NOT NULL UNIQUE,
                          description     VARCHAR(500),
                          story           TEXT,
                          price           NUMERIC(10,2)   NOT NULL CHECK (price > 0),
                          stock_type      stock_type_enum NOT NULL,
                          stock_quantity  INTEGER         CHECK (stock_quantity >= 0),
                          is_custom_order BOOLEAN         NOT NULL DEFAULT FALSE,
                          is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
                          created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
                          deleted_at      TIMESTAMPTZ,

                          CONSTRAINT chk_stock_quantity CHECK (
                              stock_type = 'MADE_TO_ORDER' OR stock_quantity IS NOT NULL
                              )
);

CREATE INDEX idx_products_slug        ON products(slug);
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_active      ON products(is_active) WHERE is_active = TRUE;
CREATE INDEX idx_products_deleted     ON products(deleted_at) WHERE deleted_at IS NULL;