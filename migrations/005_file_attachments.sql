-- Migration 005: File attachments for tickets
-- Run against: sgxloojlaywdrrglfjun (shared Supabase)
-- Date: 2026-04-21

-- Add attachments column (array of storage URLs)
ALTER TABLE user_tickets ADD COLUMN IF NOT EXISTS attachments TEXT[] DEFAULT ARRAY[]::TEXT[];

-- Create storage bucket for ticket attachments
INSERT INTO storage.buckets (id, name, public)
VALUES ('ticket-attachments', 'ticket-attachments', true)
ON CONFLICT (id) DO NOTHING;

-- RLS: anyone can read attachments (public bucket)
CREATE POLICY "Anyone can read ticket attachments"
    ON storage.objects FOR SELECT
    USING (bucket_id = 'ticket-attachments');

-- RLS: anyone can upload attachments
CREATE POLICY "Anyone can upload ticket attachments"
    ON storage.objects FOR INSERT
    WITH CHECK (bucket_id = 'ticket-attachments');

COMMENT ON COLUMN user_tickets.attachments IS 'Array of Supabase Storage URLs for attached files';
