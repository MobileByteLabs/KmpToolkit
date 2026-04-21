-- Migration 003: Create ticket_comments table for threaded discussion
-- Run against: sgxloojlaywdrrglfjun (shared Supabase)
-- Date: 2026-04-21

CREATE TABLE IF NOT EXISTS ticket_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES user_tickets(id) ON DELETE CASCADE,
    author_type TEXT NOT NULL DEFAULT 'user',
    author_name TEXT NOT NULL DEFAULT 'Anonymous',
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ticket_comments_ticket ON ticket_comments(ticket_id);

ALTER TABLE ticket_comments ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can read comments on public tickets" ON ticket_comments
    FOR SELECT USING (
        EXISTS (SELECT 1 FROM user_tickets WHERE id = ticket_id AND is_private = false)
    );

CREATE POLICY "Anyone can add comments" ON ticket_comments
    FOR INSERT WITH CHECK (true);

CREATE OR REPLACE FUNCTION add_comment(p_ticket_id UUID, p_author_type TEXT, p_author_name TEXT, p_content TEXT)
RETURNS UUID LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE new_id UUID;
BEGIN
    INSERT INTO ticket_comments (ticket_id, author_type, author_name, content)
    VALUES (p_ticket_id, p_author_type, p_author_name, p_content)
    RETURNING id INTO new_id;
    UPDATE user_tickets SET updated_at = NOW() WHERE id = p_ticket_id;
    RETURN new_id;
END;
$$;
