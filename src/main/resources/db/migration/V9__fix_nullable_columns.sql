ALTER TABLE recovery_code
ALTER COLUMN encrypted_private_key DROP NOT NULL;

ALTER TABLE recovery_code
ALTER COLUMN salt DROP NOT NULL;