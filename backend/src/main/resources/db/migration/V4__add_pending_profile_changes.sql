-- ==============================================================================
-- PrivyChat Database Schema Migration V4
-- Add pending email and phone fields for two-step verification workflows
-- ==============================================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS pending_email VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS pending_phone VARCHAR(32);

