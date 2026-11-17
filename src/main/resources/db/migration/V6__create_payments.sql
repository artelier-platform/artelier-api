-- ============================================================
-- V7: Payments
-- ============================================================

CREATE TABLE payments (
                          id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
                          order_id             UUID          NOT NULL,
                          wompi_transaction_id VARCHAR(255),
                          reference            VARCHAR(255)  NOT NULL,
                          status               VARCHAR(50)   NOT NULL,
                          payment_method       VARCHAR(50),
                          amount               NUMERIC(10,2) NOT NULL CHECK (amount > 0),
                          redirect_url         TEXT,
                          paid_at              TIMESTAMPTZ,
                          created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

                          CONSTRAINT fk_payments_order
                              FOREIGN KEY (order_id)
                                  REFERENCES orders(id)
                                  ON DELETE RESTRICT,

                          CONSTRAINT uk_payments_reference
                              UNIQUE (reference)
);

CREATE INDEX idx_payments_order_id
    ON payments(order_id);

CREATE UNIQUE INDEX uk_payments_wompi_transaction
    ON payments(wompi_transaction_id)
    WHERE wompi_transaction_id IS NOT NULL;