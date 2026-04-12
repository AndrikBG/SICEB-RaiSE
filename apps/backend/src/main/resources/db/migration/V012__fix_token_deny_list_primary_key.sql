-- V012: Fix token_deny_list primary key to comply with offline-first conventions.
-- Rationale: ArchUnit OfflineConventionsArchTest requires JPA @Id fields to be UUID/EntityId.
-- We keep jti as unique natural key for lookups, but move PK to UUID entry_id.

ALTER TABLE token_deny_list
    ADD COLUMN IF NOT EXISTS entry_id UUID NOT NULL DEFAULT gen_random_uuid();

-- Drop old PK on jti if present, then re-create on entry_id.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'token_deny_list_pkey'
          AND conrelid = 'token_deny_list'::regclass
    ) THEN
        EXECUTE 'ALTER TABLE token_deny_list DROP CONSTRAINT token_deny_list_pkey';
    END IF;
END $$;

ALTER TABLE token_deny_list
    ADD CONSTRAINT token_deny_list_pkey PRIMARY KEY (entry_id);

-- Ensure jti remains unique for fast existence checks.
CREATE UNIQUE INDEX IF NOT EXISTS ux_token_deny_list_jti ON token_deny_list(jti);

