-- Migration 001: Add priority and milestone columns to user_tickets
-- Run against: sgxloojlaywdrrglfjun (shared Supabase)
-- Date: 2026-04-21

ALTER TABLE user_tickets ADD COLUMN IF NOT EXISTS priority TEXT DEFAULT 'medium';
ALTER TABLE user_tickets ADD COLUMN IF NOT EXISTS milestone TEXT;

-- Add index for priority filtering
CREATE INDEX IF NOT EXISTS idx_user_tickets_priority ON user_tickets(priority);

-- Add index for milestone grouping (roadmap view)
CREATE INDEX IF NOT EXISTS idx_user_tickets_milestone ON user_tickets(milestone);

COMMENT ON COLUMN user_tickets.priority IS 'Ticket priority: low, medium, high, critical';
COMMENT ON COLUMN user_tickets.milestone IS 'Target milestone/version (e.g. v2.2.0)';
