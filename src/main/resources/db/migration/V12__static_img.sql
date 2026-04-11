-- Вставляем дефолтную аватарку с фиксированным ID (например, 1)
INSERT INTO attachments (id,message_id,type, url, created_at)
VALUES (1,null,'IMAGE', 'default-avatar.jpg', NOW())
ON CONFLICT (id) DO NOTHING;

-- Если у тебя id — это SERIAL или BIGSERIAL, нужно сдвинуть счетчик, 
-- чтобы следующие загрузки не пытались занять ID = 1
SELECT setval(pg_get_serial_sequence('attachments', 'id'), 1);