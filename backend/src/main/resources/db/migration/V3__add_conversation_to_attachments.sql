-- V3: Add conversation relationship and indexes to encrypted attachments
ALTER TABLE encrypted_attachments 
    ADD COLUMN IF NOT EXISTS conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_attachments_conversation 
    ON encrypted_attachments(conversation_id);

CREATE INDEX IF NOT EXISTS idx_attachments_uploader 
    ON encrypted_attachments(uploader_id);

