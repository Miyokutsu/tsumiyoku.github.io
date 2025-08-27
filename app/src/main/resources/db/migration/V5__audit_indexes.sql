CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_chain (created_at);
CREATE INDEX IF NOT EXISTS idx_audit_event ON audit_chain (event);