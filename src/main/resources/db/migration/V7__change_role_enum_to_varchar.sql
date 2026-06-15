-- V7__convert_role_to_varchar.sql

ALTER TABLE users
ALTER COLUMN role TYPE VARCHAR(20)
USING role::text;