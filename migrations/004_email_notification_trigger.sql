-- Migration 004: Email notification trigger on ticket status change
-- Run against: sgxloojlaywdrrglfjun (shared Supabase)
-- Date: 2026-04-21
--
-- This creates a DB trigger that calls a Supabase Edge Function
-- when a ticket's status changes or admin_response is added.
-- The Edge Function sends an email to user_email if provided.
--
-- NOTE: Requires deploying the Edge Function separately:
--   supabase functions deploy notify-ticket-update

-- Trigger function that fires on ticket updates
CREATE OR REPLACE FUNCTION notify_ticket_update()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    -- Only fire if status changed or admin_response was added
    IF (OLD.status IS DISTINCT FROM NEW.status) OR
       (OLD.admin_response IS DISTINCT FROM NEW.admin_response AND NEW.admin_response IS NOT NULL) THEN
        -- Only if user provided email
        IF NEW.user_email IS NOT NULL THEN
            PERFORM net.http_post(
                url := current_setting('app.settings.edge_function_url', true) || '/notify-ticket-update',
                headers := jsonb_build_object(
                    'Content-Type', 'application/json',
                    'Authorization', 'Bearer ' || current_setting('app.settings.service_role_key', true)
                ),
                body := jsonb_build_object(
                    'ticket_id', NEW.id,
                    'title', NEW.title,
                    'old_status', OLD.status,
                    'new_status', NEW.status,
                    'admin_response', NEW.admin_response,
                    'user_email', NEW.user_email
                )
            );
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

-- Create trigger (drop first if exists)
DROP TRIGGER IF EXISTS on_ticket_update ON user_tickets;
CREATE TRIGGER on_ticket_update
    AFTER UPDATE ON user_tickets
    FOR EACH ROW
    EXECUTE FUNCTION notify_ticket_update();

COMMENT ON FUNCTION notify_ticket_update IS 'Sends email notification when ticket status changes or admin responds';
