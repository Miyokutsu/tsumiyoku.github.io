-- Extensions
create extension if not exists "uuid-ossp";
create extension if not exists citext;

-- Citizens
create table if not exists citizen
(
    id                UUID primary key       default uuid_generate_v4(),
    external_id       TEXT unique,
    email             CITEXT unique not null,
    email_verified_at TIMESTAMPTZ,
    display_name      TEXT,
    status            TEXT          not null default 'ACTIVE',
    created_at        TIMESTAMPTZ   not null default now()
);

-- Assurance
create table if not exists assurance
(
    citizen_id UUID primary key references citizen (id) on delete cascade,
    ial        SMALLINT    not null default 1,
    aal        SMALLINT    not null default 1,
    updated_at TIMESTAMPTZ not null default now()
);

-- Auth tables (simplified)
create table if not exists credential_password
(
    citizen_id    UUID primary key references citizen (id) on delete cascade,
    password_hash TEXT        not null,
    algo          TEXT        not null default 'argon2id',
    updated_at    TIMESTAMPTZ not null default now(),
    failed_count  INT         not null default 0,
    locked_until  TIMESTAMPTZ
);

create table if not exists mfa_totp
(
    citizen_id   UUID primary key references citizen (id) on delete cascade,
    secret_enc   BYTEA not null,
    verified_at  TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ
);

create table if not exists webauthn_credential
(
    id              UUID primary key      default uuid_generate_v4(),
    citizen_id      UUID         not null references citizen (id) on delete cascade,
    credential_id   BYTEA unique not null,
    public_key_cose BYTEA        not null,
    sign_count      INT          not null default 0,
    aaguid          UUID,
    transports      TEXT[],
    created_at      TIMESTAMPTZ  not null default now(),
    last_used_at    TIMESTAMPTZ,
    label           TEXT
);

create table if not exists session_server
(
    id           BYTEA primary key,
    citizen_id   UUID        not null references citizen (id) on delete cascade,
    created_at   TIMESTAMPTZ not null default now(),
    last_seen_at TIMESTAMPTZ,
    expires_at   TIMESTAMPTZ not null,
    ip_hash      BYTEA,
    ua_hash      BYTEA,
    csrf_secret  BYTEA,
    revoked      BOOLEAN     not null default false
);

create table if not exists email_token
(
    token_hash BYTEA primary key,
    citizen_id UUID        not null references citizen (id) on delete cascade,
    purpose    TEXT        not null,
    expires_at TIMESTAMPTZ not null,
    used_at    TIMESTAMPTZ
);

create table if not exists auth_audit
(
    id              BIGSERIAL primary key,
    citizen_id      UUID,
    event           TEXT        not null,
    success         BOOLEAN     not null,
    ip_hash         BYTEA,
    ua_hash         BYTEA,
    meta            JSONB,
    created_at      TIMESTAMPTZ not null default now(),
    chain_prev_hmac BYTEA,
    chain_hmac      BYTEA
);

create table if not exists rate_limiter
(
    key          TEXT primary key,
    window_start TIMESTAMPTZ not null,
    count        INT         not null
);

-- Identity flows
create table if not exists identity_application
(
    id             UUID primary key     default uuid_generate_v4(),
    citizen_id     UUID        not null references citizen (id) on delete cascade,
    type           TEXT        not null,
    state          TEXT        not null default 'DRAFT',
    risk_score     INT         not null default 0,
    required_score INT         not null default 100,
    created_at     TIMESTAMPTZ not null default now(),
    decided_at     TIMESTAMPTZ,
    reviewer_id    UUID references citizen (id)
);
create index if not exists idx_identity_app_citizen on identity_application (citizen_id);
create index if not exists idx_identity_app_state on identity_application (state);

create table if not exists identity_evidence
(
    id             UUID primary key     default uuid_generate_v4(),
    application_id UUID        not null references identity_application (id) on delete cascade,
    kind           TEXT        not null,
    ref            TEXT,
    hash           BYTEA,
    enc_meta       BYTEA,
    verified       BOOLEAN     not null default false,
    comment        TEXT,
    created_at     TIMESTAMPTZ not null default now(),
    unique (application_id, kind, ref)
);
create index if not exists idx_identity_evidence_app on identity_evidence (application_id);

create table if not exists endorsement
(
    id             UUID primary key     default uuid_generate_v4(),
    application_id UUID        not null references identity_application (id) on delete cascade,
    endorser_id    UUID        not null references citizen (id) on delete cascade,
    weight         INT         not null default 1,
    signature      BYTEA       not null,
    created_at     TIMESTAMPTZ not null default now(),
    unique (application_id, endorser_id)
);
create index if not exists idx_endorsement_app on endorsement (application_id);

create table if not exists verifiable_credential
(
    id           UUID primary key     default uuid_generate_v4(),
    subject_id   UUID        not null references citizen (id) on delete cascade,
    type         TEXT        not null,
    status       TEXT        not null default 'ACTIVE',
    issued_at    TIMESTAMPTZ not null default now(),
    revoked_at   TIMESTAMPTZ,
    payload_json JSONB       not null,
    proof_jws    TEXT        not null
);
create unique index if not exists uidx_vc_subject_type_active on verifiable_credential (subject_id, type) where status = 'ACTIVE';

create table if not exists revocation_registry
(
    id         UUID primary key     default uuid_generate_v4(),
    vc_id      UUID        not null references verifiable_credential (id) on delete cascade,
    status     TEXT        not null,
    reason     TEXT,
    updated_at TIMESTAMPTZ not null default now()
);

-- Immuable audit chain
create table if not exists audit_chain
(
    id         BIGSERIAL primary key,
    citizen_id UUID,
    event      TEXT        not null,
    success    BOOLEAN     not null,
    meta       JSONB,
    created_at TIMESTAMPTZ not null default now(),
    prev_hmac  BYTEA,
    hmac       BYTEA
);
create index if not exists idx_audit_citizen on audit_chain (citizen_id);