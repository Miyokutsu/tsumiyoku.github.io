CREATE TABLE IF NOT EXISTS admin_approval
(
    citizen_id
                UUID
        PRIMARY
            KEY
        REFERENCES
            citizen
                (
                 id
                    ) ON DELETE CASCADE,
    approved_by UUID NOT NULL REFERENCES citizen
        (
         id
            ),
    roles       TEXT[]      NOT NULL DEFAULT ARRAY ['ADMIN'],
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now
        (
        )
);
CREATE INDEX IF NOT EXISTS idx_admin_approval_roles ON admin_approval USING GIN (roles);