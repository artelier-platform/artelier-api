-- ============================================================
-- V2: Categories
-- Matches: Category.java
-- ============================================================

CREATE TABLE categories (
                            id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                            name        VARCHAR(100) NOT NULL,
                            slug        VARCHAR(120) NOT NULL UNIQUE,
                            description VARCHAR(500)
);

CREATE INDEX idx_categories_slug ON categories(slug);

INSERT INTO categories (name, slug, description) VALUES
                                                     ('Cerámica',     'ceramica',     'Piezas en cerámica pintadas a mano'),
                                                     ('Madera',       'madera',       'Piezas en madera con detalle artesanal'),
                                                     ('Resina',       'resina',       'Individuales y accesorios en resina'),
                                                     ('Pedido especial', 'pedido-especial', 'Piezas hechas a pedido y personalizadas');