CREATE TABLE recovery_code (
    user_id VARCHAR(36) PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    encrypted_private_key TEXT NOT NULL,
    salt TEXT NOT NULL,
    recovery_code_hash VARCHAR(512) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);