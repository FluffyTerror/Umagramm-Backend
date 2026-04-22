ALTER TABLE chats
ALTER COLUMN avatar_url TYPE BIGINT USING avatar_url::bigint;

ALTER TABLE chats
RENAME COLUMN avatar_url TO avatar_id
