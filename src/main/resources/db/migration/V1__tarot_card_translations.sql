-- Migration: add tarot_card_translations and move name/description from tarot_cards (Option A)
-- Run this manually BEFORE deploying the new code if you have existing data in tarot_cards with name/description.
-- New installs: Hibernate ddl-auto will create schema; seed translations separately.

CREATE TABLE IF NOT EXISTS tarot_card_translations (
    id BIGSERIAL PRIMARY KEY,
    card_id INTEGER NOT NULL REFERENCES tarot_cards(id) ON DELETE CASCADE,
    locale VARCHAR(10) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    CONSTRAINT uk_tarot_card_translations_card_locale UNIQUE (card_id, locale)
);

CREATE INDEX IF NOT EXISTS idx_tarot_card_translations_card_id ON tarot_card_translations(card_id);
CREATE INDEX IF NOT EXISTS idx_tarot_card_translations_locale ON tarot_card_translations(locale);

-- Copy existing name/description into translations (locale 'en'). Skip if column already dropped.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'tarot_cards' AND column_name = 'name'
    ) THEN
        INSERT INTO tarot_card_translations (card_id, locale, name, description)
        SELECT id, 'en', name, description FROM tarot_cards
        ON CONFLICT (card_id, locale) DO NOTHING;
    END IF;
END $$;

-- Drop columns from tarot_cards (run only after copy above)
ALTER TABLE tarot_cards DROP COLUMN IF EXISTS name;
ALTER TABLE tarot_cards DROP COLUMN IF EXISTS description;

-- Drop old unique index on name if it exists (Hibernate may have created idx_tarot_cards_name)
DROP INDEX IF EXISTS idx_tarot_cards_name;
