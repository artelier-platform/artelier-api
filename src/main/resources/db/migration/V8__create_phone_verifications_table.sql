CREATE TABLE phone_verifications (
                                     id UUID PRIMARY KEY,
                                     user_id UUID NOT NULL,
                                     phone_country_code VARCHAR(4) NOT NULL,
                                     phone_number VARCHAR(15) NOT NULL,
                                     code_hash VARCHAR(255) NOT NULL,
                                     created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                     expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                     consumed_at TIMESTAMP WITH TIME ZONE,
                                     attempts INTEGER NOT NULL,

                                     CONSTRAINT fk_phone_verifications_user
                                         FOREIGN KEY (user_id)
                                             REFERENCES users(id)
                                             ON DELETE CASCADE
);

CREATE INDEX idx_phone_verifications_user_id ON phone_verifications(user_id);