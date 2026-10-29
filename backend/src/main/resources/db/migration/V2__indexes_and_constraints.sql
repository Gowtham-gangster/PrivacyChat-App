-- ==============================================================================
-- PrivyChat Database Schema Migration V2: Performance Indexes & Constraints
-- ==============================================================================

-- User lookups
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone_number);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

-- Device & Prekey lookups
CREATE INDEX IF NOT EXISTS idx_devices_user ON user_devices(user_id);
CREATE INDEX IF NOT EXISTS idx_devices_active ON user_devices(user_id, is_active);
CREATE INDEX IF NOT EXISTS idx_otpk_available ON one_time_prekeys(device_id) WHERE NOT consumed;

-- Conversation lookups
CREATE INDEX IF NOT EXISTS idx_conv_part_user ON conversation_participants(user_id);
CREATE INDEX IF NOT EXISTS idx_conv_part_conv ON conversation_participants(conversation_id);
CREATE INDEX IF NOT EXISTS idx_conv_updated ON conversations(updated_at DESC);

-- Message queuing and sync
CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(conversation_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_pending_delivery ON messages(recipient_id) WHERE NOT is_delivered;
CREATE INDEX IF NOT EXISTS idx_messages_unread ON messages(conversation_id, recipient_id) WHERE NOT is_read;

-- Attachments, Invitations, Calls
CREATE INDEX IF NOT EXISTS idx_attachments_uploader ON encrypted_attachments(uploader_id);
CREATE INDEX IF NOT EXISTS idx_attachments_message ON encrypted_attachments(message_id);
CREATE INDEX IF NOT EXISTS idx_invitations_token ON invitations(token_hash);
CREATE INDEX IF NOT EXISTS idx_invitations_email ON invitations(recipient_email);
CREATE INDEX IF NOT EXISTS idx_calls_caller ON call_sessions(caller_id);
CREATE INDEX IF NOT EXISTS idx_calls_callee ON call_sessions(callee_id);
CREATE INDEX IF NOT EXISTS idx_blocked_blocker ON blocked_users(blocker_id);
CREATE INDEX IF NOT EXISTS idx_verification_identifier ON verification_codes(identifier, type);

