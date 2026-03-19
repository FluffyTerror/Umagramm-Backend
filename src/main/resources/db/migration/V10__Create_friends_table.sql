CREATE TABLE friend_request(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id VARCHAR(36) FOREIGN KEY REFERENCES users(id) ON DELETE CASCADE,
    receiver_id VARCHAR(36) FOREIGN KEY REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(36) NOT NULL CHECK( status IN ('ACCEPTED','PENDING','DECLINED'))
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP

    CONSTRAINT check_not_self CHECK (sender_id <> receiver_id),
    CONSTRAINT unique_request UNIQUE (sender_id, receiver_id)
);

CREATE TABLE friends(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id VARCHAR(36) FOREIGN KEY REFERENCES users(id) ON DELETE CASCADE,
    receiver_id VARCHAR(36) FOREIGN KEY REFERENCES users(id) ON DELETE CASCADE,
    friends_since TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT check_not_self CHECK (sender_id <> receiver_id),
        CONSTRAINT unique_friendship UNIQUE (
            LEAST(user1_id, user2_id),
            GREATEST(user1_id, user2_id)
        )
)
