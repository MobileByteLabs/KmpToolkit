-- Migration: Rename feature_requests → user_tickets + add support ticket columns
-- Target: Shared Supabase (sgxloojlaywdrrglfjun.supabase.co)
-- Run via: Supabase Dashboard → SQL Editor

-- Step 1: Rename table
ALTER TABLE feature_requests RENAME TO user_tickets;

-- Step 2: Add new columns
ALTER TABLE user_tickets
  ADD COLUMN ticket_type TEXT NOT NULL DEFAULT 'feature_request',
  ADD COLUMN is_private BOOLEAN NOT NULL DEFAULT false,
  ADD COLUMN user_id TEXT,
  ADD COLUMN admin_response TEXT,
  ADD COLUMN responded_at TIMESTAMPTZ;

-- Step 3: Backfill existing rows — bug_fix category becomes bug_report type
UPDATE user_tickets SET ticket_type = 'bug_report' WHERE category = 'bug_fix';

-- Step 4: Indexes
CREATE INDEX idx_user_tickets_type ON user_tickets(ticket_type);
CREATE INDEX idx_user_tickets_product_type ON user_tickets(product_type, ticket_type);
CREATE INDEX idx_user_tickets_user_private ON user_tickets(user_id, is_private)
  WHERE is_private = true;

-- Step 5: Drop old RLS policies (if any)
DROP POLICY IF EXISTS "Enable read access for all users" ON user_tickets;
DROP POLICY IF EXISTS "Enable insert for all users" ON user_tickets;

-- Step 6: Enable RLS
ALTER TABLE user_tickets ENABLE ROW LEVEL SECURITY;

-- Step 7: RLS Policies
-- Public tickets (feature_request + bug_report) visible to all
CREATE POLICY "public_tickets_select" ON user_tickets
  FOR SELECT USING (is_private = false);

-- Private tickets visible only to creator via x-user-id header
CREATE POLICY "private_tickets_select" ON user_tickets
  FOR SELECT USING (
    is_private = true
    AND user_id = coalesce(
      current_setting('request.headers', true)::json->>'x-user-id',
      ''
    )
  );

-- Anyone can create tickets
CREATE POLICY "tickets_insert" ON user_tickets
  FOR INSERT WITH CHECK (true);

-- Step 8: Rename upvote RPC
CREATE OR REPLACE FUNCTION upvote_ticket(ticket_id text)
RETURNS void AS $$
  UPDATE user_tickets SET upvotes = upvotes + 1 WHERE id = ticket_id AND is_private = false;
$$ LANGUAGE sql SECURITY DEFINER;

-- Drop old RPC
DROP FUNCTION IF EXISTS upvote_feature(text);
