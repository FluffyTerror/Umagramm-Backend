CREATE TABLE recovery_code (
    user_id VARCHAR(36) PRIMARY KEY,
    encrypted_private_key TEXT NOT NULL,
    salt TEXT NOT NULL,
    recovery_code_hash VARCHAR(512) NOT NULL, -- Хэш от присланного клиентом Proof
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);