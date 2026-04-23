-- Supabase migration 001: Create product_tickets table
-- Canonical base migration for KmpToolkit consumer projects.
-- Full annotated version: layers/tickets/templates/migrations/000_create_product_tickets.sql

CREATE TABLE IF NOT EXISTS product_tickets (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_type      TEXT NOT NULL DEFAULT 'feature_request',
    title            TEXT NOT NULL,
    description      TEXT,
    category         TEXT DEFAULT 'general',
    status           TEXT DEFAULT 'pending',
    priority         TEXT DEFAULT 'medium',
    severity         TEXT,
    platform         TEXT,
    app_version      TEXT,
    milestone        TEXT,
    labels           TEXT[] DEFAULT ARRAY[]::TEXT[],
    attachments      TEXT[] DEFAULT ARRAY[]::TEXT[],
    is_private       BOOLEAN NOT NULL DEFAULT false,
    user_id          TEXT,
    user_email       TEXT,
    device_info      JSONB,
    upvotes          INT NOT NULL DEFAULT 0,
    admin_response   TEXT,
    responded_at     TIMESTAMPTZ,
    resolution       TEXT,
    internal_notes   TEXT,
    assignee         TEXT,
    created_at       TIMESTAMPTZ DEFAULT NOW(),
    updated_at       TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE product_tickets ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public tickets readable by anyone" ON product_tickets FOR SELECT USING (is_private = false);
CREATE POLICY "Anyone can submit a ticket" ON product_tickets FOR INSERT WITH CHECK (true);
CREATE POLICY "Service role full access" ON product_tickets FOR ALL USING (auth.role() = 'service_role');

CREATE INDEX IF NOT EXISTS idx_product_tickets_status   ON product_tickets(status);
CREATE INDEX IF NOT EXISTS idx_product_tickets_priority ON product_tickets(priority);
CREATE INDEX IF NOT EXISTS idx_product_tickets_type     ON product_tickets(ticket_type);
CREATE INDEX IF NOT EXISTS idx_product_tickets_created  ON product_tickets(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_product_tickets_votes    ON product_tickets(upvotes DESC);

CREATE OR REPLACE FUNCTION upvote_ticket(p_ticket_id UUID)
RETURNS INT LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE new_count INT;
BEGIN
    UPDATE product_tickets SET upvotes = upvotes + 1, updated_at = NOW() WHERE id = p_ticket_id;
    SELECT upvotes INTO new_count FROM product_tickets WHERE id = p_ticket_id;
    RETURN new_count;
END;
$$;
