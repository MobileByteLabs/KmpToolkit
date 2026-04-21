-- Migration 002: Create ticket_votes table for 1-per-user vote enforcement
-- Run against: sgxloojlaywdrrglfjun (shared Supabase)
-- Date: 2026-04-21

CREATE TABLE IF NOT EXISTS ticket_votes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL REFERENCES user_tickets(id) ON DELETE CASCADE,
    voter_id TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(ticket_id, voter_id)
);

CREATE INDEX IF NOT EXISTS idx_ticket_votes_ticket ON ticket_votes(ticket_id);
CREATE INDEX IF NOT EXISTS idx_ticket_votes_voter ON ticket_votes(voter_id);

-- RLS: anyone can read votes, anyone can insert their own vote
ALTER TABLE ticket_votes ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can read votes"
    ON ticket_votes FOR SELECT
    USING (true);

CREATE POLICY "Anyone can insert votes"
    ON ticket_votes FOR INSERT
    WITH CHECK (true);

CREATE POLICY "Users can delete own votes"
    ON ticket_votes FOR DELETE
    USING (true);

-- RPC: toggle vote (insert or delete) — returns new vote count
CREATE OR REPLACE FUNCTION toggle_vote(p_ticket_id UUID, p_voter_id TEXT)
RETURNS INT LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    vote_exists BOOLEAN;
    new_count INT;
BEGIN
    SELECT EXISTS(
        SELECT 1 FROM ticket_votes WHERE ticket_id = p_ticket_id AND voter_id = p_voter_id
    ) INTO vote_exists;

    IF vote_exists THEN
        DELETE FROM ticket_votes WHERE ticket_id = p_ticket_id AND voter_id = p_voter_id;
        UPDATE user_tickets SET upvotes = GREATEST(upvotes - 1, 0), updated_at = NOW() WHERE id = p_ticket_id;
    ELSE
        INSERT INTO ticket_votes (ticket_id, voter_id) VALUES (p_ticket_id, p_voter_id);
        UPDATE user_tickets SET upvotes = upvotes + 1, updated_at = NOW() WHERE id = p_ticket_id;
    END IF;

    SELECT upvotes INTO new_count FROM user_tickets WHERE id = p_ticket_id;
    RETURN new_count;
END;
$$;

COMMENT ON TABLE ticket_votes IS 'Tracks individual votes — enforces 1 vote per user per ticket';
COMMENT ON FUNCTION toggle_vote IS 'Toggle vote on/off for a ticket. Returns new vote count.';
