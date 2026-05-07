CREATE TABLE payments
(
    id                   UUID NOT NULL,
    order_id             UUID NOT NULL,
    wompi_transaction_id VARCHAR(255),
    reference            VARCHAR(255),
    status               VARCHAR(255),
    payment_method       VARCHAR(255),
    amount               DECIMAL,
    paid_at              TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_payments PRIMARY KEY (id)
);

ALTER TABLE payments
    ADD CONSTRAINT uc_payments_order UNIQUE (order_id);

ALTER TABLE payments
    ADD CONSTRAINT FK_PAYMENTS_ON_ORDER FOREIGN KEY (order_id) REFERENCES orders (id);