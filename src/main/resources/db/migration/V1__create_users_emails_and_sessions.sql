-- ============================================================
-- V1: Users + User Emails + User Sessions
-- ============================================================

-- ============================================================
-- ENUMS
-- ============================================================

CREATE TYPE user_role_enum AS ENUM (
    'ADMIN',
    'BUYER'
);

CREATE TYPE user_status_enum AS ENUM (
    'PENDING_VERIFICATION',
    'ACTIVE',
    'SUSPENDED'
);

CREATE TYPE session_revocation_reason_enum AS ENUM (
    'USER_LOGOUT',
    'LOGOUT_ALL',
    'PASSWORD_CHANGED',
    'ACCOUNT_SUSPENDED',
    'SESSION_EXPIRED',
    'SECURITY_ACTION',
    'ADMIN_ACTION'
);

-- ============================================================
-- USERS
-- ============================================================

CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                       username VARCHAR(12) NOT NULL,

                       password_hash VARCHAR(255) NOT NULL,

                       first_name VARCHAR(58) NOT NULL,
                       last_name VARCHAR(34) NOT NULL,

                       phone_country_code VARCHAR(4) NOT NULL,
                       phone_number VARCHAR(15) NOT NULL,

                       phone_verified_at TIMESTAMPTZ,

                       profile_image_url VARCHAR(500),
                       profile_image_public_id VARCHAR(255),

                       status user_status_enum NOT NULL
                                           DEFAULT 'PENDING_VERIFICATION',

                       role user_role_enum NOT NULL
                                           DEFAULT 'BUYER',

                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                       CONSTRAINT uk_users_username UNIQUE (username),

                       CONSTRAINT chk_users_username_format
                           CHECK (username ~ '^[!-~]{1,12}$'),

    CONSTRAINT chk_users_first_name
        CHECK (LENGTH(BTRIM(first_name)) BETWEEN 1 AND 58),

    CONSTRAINT chk_users_last_name
        CHECK (LENGTH(BTRIM(last_name)) BETWEEN 1 AND 34),

    CONSTRAINT chk_users_phone_country_code
        CHECK (LENGTH(BTRIM(phone_country_code)) BETWEEN 1 AND 4),

    CONSTRAINT chk_users_phone_number
        CHECK (LENGTH(BTRIM(phone_number)) BETWEEN 1 AND 15)
);

CREATE INDEX idx_users_status
    ON users(status);

CREATE INDEX idx_users_created_at
    ON users(created_at);

-- ============================================================
-- USER EMAILS
-- ============================================================

CREATE TABLE user_emails (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                             user_id UUID NOT NULL,

                             email VARCHAR(254) NOT NULL,

                             is_primary BOOLEAN NOT NULL DEFAULT FALSE,

                             is_verified BOOLEAN NOT NULL DEFAULT FALSE,

                             verified_at TIMESTAMPTZ,

                             created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                             CONSTRAINT fk_user_emails_user
                                 FOREIGN KEY (user_id)
                                     REFERENCES users(id)
                                     ON DELETE CASCADE,

                             CONSTRAINT uk_user_emails_email
                                 UNIQUE (email),

                             CONSTRAINT chk_user_emails_verification
                                 CHECK (
                                     is_verified = (verified_at IS NOT NULL)
                                     ),

                             CONSTRAINT chk_user_emails_primary_verified
                                 CHECK (
                                     NOT is_primary OR is_verified
                                     )
    -- AQUÍ SE ELIMINÓ LA LÍNEA DUPLICADA Y LA COMA SOBRANTE
);

CREATE INDEX idx_user_emails_user_id
    ON user_emails(user_id);

CREATE INDEX idx_user_emails_verified
    ON user_emails(user_id, is_verified);

-- Un usuario solamente puede tener un correo principal.
CREATE UNIQUE INDEX uk_user_emails_primary
    ON user_emails(user_id)
    WHERE is_primary = TRUE;

CREATE UNIQUE INDEX uk_users_username_lower ON users (lower(username));

-- ============================================================
-- USER SESSIONS
-- ============================================================

CREATE TABLE user_sessions (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                               user_id UUID NOT NULL,

                               refresh_token_hash VARCHAR(255) NOT NULL,

                               created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                               last_used_at TIMESTAMPTZ,

                               expires_at TIMESTAMPTZ NOT NULL,

                               revoked_at TIMESTAMPTZ,

                               revocation_reason session_revocation_reason_enum,

                               CONSTRAINT fk_user_sessions_user
                                   FOREIGN KEY (user_id)
                                       REFERENCES users(id)
                                       ON DELETE CASCADE,

                               CONSTRAINT uk_user_sessions_refresh_token_hash
                                   UNIQUE (refresh_token_hash),

                               CONSTRAINT chk_user_sessions_revocation
                                   CHECK (
                                       (revoked_at IS NULL) =
                                       (revocation_reason IS NULL)
                                       )
);

CREATE INDEX idx_user_sessions_user_id
    ON user_sessions(user_id);

CREATE INDEX idx_user_sessions_expires_at
    ON user_sessions(expires_at);

-- Consultas de sesiones no revocadas.
CREATE INDEX idx_user_sessions_active
    ON user_sessions(user_id, expires_at)
    WHERE revoked_at IS NULL;

-- ============================================================
-- END V1
-- ============================================================