-- Migration 006: Rename user_tickets → product_tickets
-- Run against: shared Supabase (sgxloojlaywdrrglfjun)
-- Date: 2026-04-22
--
-- Renames the table for projects already using user_tickets.
-- New projects should use 000_create_product_tickets.sql directly.
-- product_type column is left in place (soft removal — existing data preserved).

-- Rename main table
ALTER TABLE IF EXISTS user_tickets RENAME TO product_tickets;

-- Rename indexes
ALTER INDEX IF EXISTS idx_user_tickets_priority   RENAME TO idx_product_tickets_priority;
ALTER INDEX IF EXISTS idx_user_tickets_milestone  RENAME TO idx_product_tickets_milestone;
ALTER INDEX IF EXISTS idx_user_tickets_user       RENAME TO idx_product_tickets_user;

-- Drop the product_type index (no longer used — each project has own Supabase)
DROP INDEX IF EXISTS idx_user_tickets_product;

-- Update foreign keys in supporting tables
ALTER TABLE IF EXISTS ticket_votes
    RENAME CONSTRAINT ticket_votes_ticket_id_fkey
    TO ticket_votes_product_ticket_id_fkey;

ALTER TABLE IF EXISTS ticket_comments
    RENAME CONSTRAINT ticket_comments_ticket_id_fkey
    TO ticket_comments_product_ticket_id_fkey;

-- Add new columns from 000_create_product_tickets (if not already present)
ALTER TABLE product_tickets ADD COLUMN IF NOT EXISTS severity       TEXT;
ALTER TABLE product_tickets ADD COLUMN IF NOT EXISTS platform       TEXT;
ALTER TABLE product_tickets ADD COLUMN IF NOT EXISTS app_version    TEXT;
ALTER TABLE product_tickets ADD COLUMN IF NOT EXISTS internal_notes TEXT;
ALTER TABLE product_tickets ADD COLUMN IF NOT EXISTS assignee       TEXT;
ALTER TABLE product_tickets ADD COLUMN IF NOT EXISTS device_info    JSONB;
ALTER TABLE product_tickets ADD COLUMN IF NOT EXISTS labels         TEXT[] DEFAULT ARRAY[]::TEXT[];

-- RLS: add service_role policy if missing
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE tablename = 'product_tickets' AND policyname = 'Service role full access'
    ) THEN
        CREATE POLICY "Service role full access"
            ON product_tickets FOR ALL
            USING (auth.role() = 'service_role');
    END IF;
END $$;

-- Auto-update trigger
CREATE OR REPLACE FUNCTION update_product_tickets_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$;

DROP TRIGGER IF EXISTS trg_product_tickets_updated_at ON product_tickets;
CREATE TRIGGER trg_product_tickets_updated_at
    BEFORE UPDATE ON product_tickets
    FOR EACH ROW EXECUTE FUNCTION update_product_tickets_updated_at();

COMMENT ON TABLE product_tickets IS 'Renamed from user_tickets — product tickets per project';
