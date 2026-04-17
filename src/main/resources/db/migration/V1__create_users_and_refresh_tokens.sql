-- ============================================================
-- V1: Users + Refresh Tokens
-- Matches: User.java, RefreshToken.java
-- ============================================================

CREATE TABLE users (
                       id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                       email         VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       full_name     VARCHAR(255),
                       phone         VARCHAR(20),
                       role          VARCHAR(20)  NOT NULL DEFAULT 'BUYER',
                       banned        BOOLEAN      NOT NULL DEFAULT FALSE,
                       created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users(email);

CREATE TABLE refresh_tokens (
                                id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                                token      TEXT        NOT NULL UNIQUE,
                                user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);