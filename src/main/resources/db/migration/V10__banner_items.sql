-- V10: el banner pasa de ser plano a contenedor (banners) + slides (banner_items).
-- V9 creó una tabla `banners` plana que ya no corresponde a las entidades.

CREATE TABLE banner_items (
    id                             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    banner_id                      UUID          NOT NULL REFERENCES banners(id) ON DELETE CASCADE,
    is_active                      BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_order                     INTEGER       NOT NULL DEFAULT 1,
    title                          VARCHAR(120),
    show_title                     BOOLEAN       NOT NULL DEFAULT TRUE,
    background_image_url           VARCHAR(2048),
    background_image_cloudinary_id VARCHAR(255),
    model_3d_url                   VARCHAR(2048),
    model_3d_public_id             VARCHAR(255),
    description                    VARCHAR(300),
    show_description               BOOLEAN       NOT NULL DEFAULT FALSE,
    button_label                   VARCHAR(40),
    button_url                     VARCHAR(500),
    show_button                    BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_banner_item_background
        CHECK (background_image_url IS NOT NULL OR model_3d_url IS NOT NULL)
);

CREATE INDEX idx_banner_items_order ON banner_items(banner_id, sort_order);

-- Si ya había filas con el modelo plano de V9, cada una pasa a ser un banner con un solo item.
INSERT INTO banner_items (
    banner_id, is_active, sort_order,
    title, show_title,
    background_image_url, background_image_cloudinary_id,
    description, show_description,
    button_label, button_url, show_button
)
SELECT
    b.id, TRUE, 1,
    b.title, TRUE,
    b.image_url, b.cloudinary_id,
    b.description, TRUE,
    b.button_label, b.button_url, (b.button_label IS NOT NULL)
FROM banners b;

DROP INDEX IF EXISTS idx_banners_active_sort;
ALTER TABLE banners DROP CONSTRAINT IF EXISTS chk_banner_button;
ALTER TABLE banners
    DROP COLUMN IF EXISTS title,
    DROP COLUMN IF EXISTS description,
    DROP COLUMN IF EXISTS image_url,
    DROP COLUMN IF EXISTS cloudinary_id,
    DROP COLUMN IF EXISTS button_label,
    DROP COLUMN IF EXISTS button_url,
    DROP COLUMN IF EXISTS sort_order;

CREATE INDEX idx_banners_active_created ON banners(is_active, created_at);

ALTER TABLE banner_items ENABLE ROW LEVEL SECURITY;
CREATE POLICY banner_items_all_access ON banner_items FOR ALL USING (true) WITH CHECK (true);
