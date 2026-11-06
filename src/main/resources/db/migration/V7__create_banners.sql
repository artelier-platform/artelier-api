-- ============================================================
-- V9: Banners
-- ============================================================

CREATE TABLE banners (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         is_active BOOLEAN NOT NULL DEFAULT TRUE,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_banners_active_created
    ON banners(is_active, created_at);

-- ============================================================
-- Banner Items
-- ============================================================

CREATE TABLE banner_items (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                              banner_id UUID NOT NULL,

                              is_active BOOLEAN NOT NULL DEFAULT TRUE,
                              sort_order INTEGER NOT NULL DEFAULT 1,

                              title VARCHAR(120),
                              show_title BOOLEAN NOT NULL DEFAULT TRUE,

                              background_image_url VARCHAR(2048),
                              background_image_cloudinary_id VARCHAR(255),

                              model_3d_url VARCHAR(2048),
                              model_3d_public_id VARCHAR(255),

                              description VARCHAR(300),
                              show_description BOOLEAN NOT NULL DEFAULT FALSE,

                              button_label VARCHAR(40),
                              button_url VARCHAR(500),
                              show_button BOOLEAN NOT NULL DEFAULT FALSE,

                              CONSTRAINT fk_banner_items_banner
                                  FOREIGN KEY (banner_id)
                                      REFERENCES banners(id)
                                      ON DELETE CASCADE,

                              CONSTRAINT chk_banner_item_background
                                  CHECK (
                                      background_image_url IS NOT NULL
                                          OR model_3d_url IS NOT NULL
                                      )
);

CREATE INDEX idx_banner_items_order
    ON banner_items(banner_id, sort_order);