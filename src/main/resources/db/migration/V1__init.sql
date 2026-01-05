-- Таблица пользователей
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    last_seen TIMESTAMP,
    profile_photo_id BIGINT,
    created_date TIMESTAMP NOT NULL,
    last_modified_date TIMESTAMP
);

-- Таблица чатов
CREATE TABLE chats (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255), -- NULL для приватных
    type VARCHAR(50) NOT NULL, -- PRIVATE, GROUP
    created_date TIMESTAMP NOT NULL,
    last_modified_date TIMESTAMP
);

-- Таблица участников (Связь Many-to-Many)
CREATE TABLE chat_members (
    id BIGSERIAL PRIMARY KEY,
    chat_id VARCHAR(36) NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Таблица сообщений
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    chat_id VARCHAR(36) NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    sender_id VARCHAR(36) NOT NULL REFERENCES users(id),
    content TEXT, -- Используем TEXT для длинных зашифрованных строк
    message_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL, -- SENT, SEEN
    created_date TIMESTAMP NOT NULL,
    last_modified_date TIMESTAMP
);

-- Таблица вложений
CREATE TABLE attachments (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT REFERENCES messages(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    url TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Таблица Refresh-токенов
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE
);

-- Индексы для ускорения поиска
CREATE INDEX idx_chat_members_user ON chat_members(user_id);
CREATE INDEX idx_messages_chat ON messages(chat_id);