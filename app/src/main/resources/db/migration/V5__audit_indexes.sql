create index if not exists idx_audit_created_at on audit_chain (created_at);
create index if not exists idx_audit_event on audit_chain (event);