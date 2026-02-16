-- Chạy script này nếu tarot_cards vẫn còn cột name/description (schema cũ).
-- Sau khi chạy, import từ Google Sheet sẽ không còn lỗi NOT NULL trên name.

ALTER TABLE tarot_cards DROP COLUMN IF EXISTS name;
ALTER TABLE tarot_cards DROP COLUMN IF EXISTS description;
DROP INDEX IF EXISTS idx_tarot_cards_name;
