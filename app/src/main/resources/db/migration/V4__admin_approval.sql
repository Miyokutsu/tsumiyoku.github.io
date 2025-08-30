create table if not exists admin_approval
(
    citizen_id  UUID primary key references citizen (id) on delete cascade,
    approved_by UUID        not null references citizen (id),
    roles       TEXT[]      not null default array ['ADMIN'],
    expires_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ not null default now()
);
create index if not exists idx_admin_approval_roles on admin_approval using GIN (roles);