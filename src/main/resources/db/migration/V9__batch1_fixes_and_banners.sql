-- Busqueda sin tildes (productos, usuarios, ordenes)
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Pagos: permitir reintentos tras un pago DECLINED (antes order_id era UNIQUE)
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_order_id_key;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now();
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);

-- Ordenes: telefono de contacto + indice para el job de expiracion y filtros por fecha
ALTER TABLE orders ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(20);
CREATE INDEX IF NOT EXISTS idx_orders_status_created_at ON orders(status, created_at);

-- Banners
CREATE TABLE banners (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    title         VARCHAR(120) NOT NULL,
    description   VARCHAR(300) NOT NULL,
    image_url     TEXT         NOT NULL,
    cloudinary_id TEXT,
    button_label  VARCHAR(40),
    button_url    VARCHAR(500),
    sort_order    INTEGER      NOT NULL DEFAULT 0,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_banner_button CHECK ((button_label IS NULL) = (button_url IS NULL))
);
CREATE INDEX idx_banners_active_sort ON banners(is_active, sort_order);
ALTER TABLE banners ENABLE ROW LEVEL SECURITY;
CREATE POLICY banners_all_access ON banners FOR ALL USING (true) WITH CHECK (true);
