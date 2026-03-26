CREATE TABLE friends (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'Pending' CHECK (status IN ('Pending', 'Accepted', 'Declined')),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT check_not_self CHECK (sender_id <> receiver_id)
);

-- 2. Создаем УНИКАЛЬНЫЙ ИНДЕКС для проверки пары (А-Б и Б-А)
-- Важно: в Postgres выражения в индексах должны быть в дополнительных скобках
CREATE UNIQUE INDEX unique_friend_pair_idx ON friends (
    (LEAST(sender_id, receiver_id)), 
    (GREATEST(sender_id, receiver_id))
);