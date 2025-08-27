-- Extensions
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE
EXTENSION IF NOT EXISTS citext;

-- Citizens
CREATE TABLE IF NOT EXISTS citizen
(
    id
                 UUID
        PRIMARY
            KEY
                               DEFAULT
                                   uuid_generate_v4
                                   (
                                   ),
    external_id  TEXT UNIQUE,
    email CITEXT UNIQUE NOT NULL,
    email_verified_at TIMESTAMPTZ,
    display_name TEXT,
    status       TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now
        (
        )
);

-- Assurance
CREATE TABLE IF NOT EXISTS assurance
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
    ial SMALLINT NOT NULL DEFAULT 1,
    aal SMALLINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now
        (
        )
);

-- Auth tables (simplified)
CREATE TABLE IF NOT EXISTS credential_password
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
    password_hash TEXT NOT NULL,
    algo          TEXT NOT NULL DEFAULT 'argon2id',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now
        (
        ),
    failed_count  INT  NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS mfa_totp
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
    secret_enc BYTEA NOT NULL,
    verified_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS webauthn_credential
(
    id
               UUID
        PRIMARY
            KEY
                             DEFAULT
                                 uuid_generate_v4
                                 (
                                 ),
    citizen_id UUID NOT NULL REFERENCES citizen
        (
         id
            ) ON DELETE CASCADE,
    credential_id BYTEA UNIQUE NOT NULL,
    public_key_cose BYTEA NOT NULL,
    sign_count INT  NOT NULL DEFAULT 0,
    aaguid     UUID,
    transports TEXT[],
    created_at TIMESTAMPTZ NOT NULL DEFAULT now
        (
        ),
    last_used_at TIMESTAMPTZ,
    label      TEXT
);

CREATE TABLE IF NOT EXISTS session_server
(
    id
        BYTEA
        PRIMARY
        KEY,
    citizen_id
            UUID
                    NOT
                        NULL
        REFERENCES
            citizen
                (
                 id
                    ) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now
        (
        ),
    last_seen_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL,
    ip_hash BYTEA,
    ua_hash BYTEA,
    csrf_secret BYTEA,
    revoked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS email_token
(
    token_hash
        BYTEA
        PRIMARY
        KEY,
    citizen_id
            UUID
                 NOT
                     NULL
        REFERENCES
            citizen
                (
                 id
                    ) ON DELETE CASCADE,
    purpose TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS auth_audit
(
    id
        BIGSERIAL
        PRIMARY
        KEY,
    citizen_id
        UUID,
    event
        TEXT
        NOT
            NULL,
    success
        BOOLEAN
        NOT
            NULL,
    ip_hash
        BYTEA,
    ua_hash
        BYTEA,
    meta
        JSONB,
    created_at
        TIMESTAMPTZ
        NOT
        NULL
        DEFAULT
        now
        (
        ),
    chain_prev_hmac BYTEA,
    chain_hmac BYTEA
);

CREATE TABLE IF NOT EXISTS rate_limiter
(
    key
        TEXT
        PRIMARY
        KEY,
    window_start
        TIMESTAMPTZ
        NOT
        NULL,
    count
        INT
        NOT
            NULL
);

-- Identity flows
CREATE TABLE IF NOT EXISTS identity_application
(
    id
                   UUID
        PRIMARY
            KEY
                                 DEFAULT
                                     uuid_generate_v4
                                     (
                                     ),
    citizen_id     UUID NOT NULL REFERENCES citizen
        (
         id
            ) ON DELETE CASCADE,
    type           TEXT NOT NULL,
    state          TEXT NOT NULL DEFAULT 'DRAFT',
    risk_score     INT  NOT NULL DEFAULT 0,
    required_score INT  NOT NULL DEFAULT 100,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now
        (
        ),
    decided_at TIMESTAMPTZ,
    reviewer_id    UUID REFERENCES citizen
        (
         id
            )
);
CREATE INDEX IF NOT EXISTS idx_identity_app_citizen ON identity_application (citizen_id);
CREATE INDEX IF NOT EXISTS idx_identity_app_state ON identity_application (state);

CREATE TABLE IF NOT EXISTS identity_evidence
(
    id
                   UUID
        PRIMARY
            KEY
                                    DEFAULT
                                        uuid_generate_v4
                                        (
                                        ),
    application_id UUID    NOT NULL REFERENCES identity_application
        (
         id
            ) ON DELETE CASCADE,
    kind           TEXT    NOT NULL,
    ref            TEXT,
    hash BYTEA,
    enc_meta BYTEA,
    verified       BOOLEAN NOT NULL DEFAULT FALSE,
    comment        TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now
        (
        ),
    UNIQUE
        (
         application_id,
         kind,
         ref
            )
);
CREATE INDEX IF NOT EXISTS idx_identity_evidence_app ON identity_evidence (application_id);

CREATE TABLE IF NOT EXISTS endorsement
(
    id
                   UUID
        PRIMARY
            KEY
                                 DEFAULT
                                     uuid_generate_v4
                                     (
                                     ),
    application_id UUID NOT NULL REFERENCES identity_application
        (
         id
            ) ON DELETE CASCADE,
    endorser_id    UUID NOT NULL REFERENCES citizen
        (
         id
            )
        ON DELETE CASCADE,
    weight         INT  NOT NULL DEFAULT 1,
    signature BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now
        (
        ),
    UNIQUE
        (
         application_id,
         endorser_id
            )
);
CREATE INDEX IF NOT EXISTS idx_endorsement_app ON endorsement (application_id);

CREATE TABLE IF NOT EXISTS verifiable_credential
(
    id
               UUID
        PRIMARY
            KEY
                             DEFAULT
                                 uuid_generate_v4
                                 (
                                 ),
    subject_id UUID NOT NULL REFERENCES citizen
        (
         id
            ) ON DELETE CASCADE,
    type       TEXT NOT NULL,
    status     TEXT NOT NULL DEFAULT 'ACTIVE',
    issued_at TIMESTAMPTZ NOT NULL DEFAULT now
        (
        ),
    revoked_at TIMESTAMPTZ,
    payload_json JSONB NOT NULL,
    proof_jws  TEXT NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_vc_subject_type_active
    ON verifiable_credential (subject_id, type) WHERE status = 'ACTIVE';

CREATE TABLE IF NOT EXISTS revocation_registry
(
    id
           UUID
        PRIMARY
            KEY
        DEFAULT
            uuid_generate_v4
            (
            ),
    vc_id  UUID NOT NULL REFERENCES verifiable_credential
        (
         id
            ) ON DELETE CASCADE,
    status TEXT NOT NULL,
    reason TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now
        (
        )
);

-- Immuable audit chain
CREATE TABLE IF NOT EXISTS audit_chain
(
    id
        BIGSERIAL
        PRIMARY
        KEY,
    citizen_id
        UUID,
    event
        TEXT
        NOT
            NULL,
    success
        BOOLEAN
        NOT
            NULL,
    meta
        JSONB,
    created_at
        TIMESTAMPTZ
        NOT
        NULL
        DEFAULT
        now
        (
        ),
    prev_hmac BYTEA,
    hmac BYTEA
);
CREATE INDEX IF NOT EXISTS idx_audit_citizen ON audit_chain (citizen_id);