-- 1. Удаляем старое дефолтное значение
ALTER TABLE friends ALTER COLUMN id DROP DEFAULT;

-- 2. Меняем тип на VARCHAR (преобразуя существующие данные в текст)
ALTER TABLE friends ALTER COLUMN id TYPE VARCHAR(36) USING id::text;

-- 3. Возвращаем дефолтное значение, но с кастом в текст
ALTER TABLE friends ALTER COLUMN id SET DEFAULT gen_random_uuid()::text;