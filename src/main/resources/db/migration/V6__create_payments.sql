-- ============================================================
-- V7: Payments
-- ============================================================

CREATE TABLE payments (
                          id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
                          order_id             UUID          NOT NULL UNIQUE REFERENCES orders(id),
                          wompi_transaction_id VARCHAR(255),
                          reference            VARCHAR(255)  NOT NULL UNIQUE,
                          status               VARCHAR(50)   NOT NULL,
                          payment_method       VARCHAR(50),
                          amount               NUMERIC(10,2) NOT NULL,
                          redirect_url         TEXT,
                          paid_at              TIMESTAMPTZ
);