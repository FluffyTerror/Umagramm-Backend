

ALTER TABLE chat_members
ADD invited_by VARCHAR(36);

ALTER TABLE chats
ADD key_version INT DEFAULT 1;