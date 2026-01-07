

ALTER TABLE refresh_tokens
ALTER COLUMN id SET DEFAULT gen_random_uuid();