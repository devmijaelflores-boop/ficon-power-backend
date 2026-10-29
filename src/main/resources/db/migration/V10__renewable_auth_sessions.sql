ALTER TABLE public.auth_sessions
    ADD COLUMN refresh_token_hash varchar(128),
    ADD COLUMN refresh_expires_at timestamptz,
    ADD COLUMN last_refreshed_at timestamptz;

CREATE UNIQUE INDEX uq_auth_sessions_refresh_token
    ON public.auth_sessions (refresh_token_hash)
    WHERE refresh_token_hash IS NOT NULL;

CREATE INDEX idx_auth_sessions_refresh_active
    ON public.auth_sessions (refresh_token_hash, refresh_expires_at, revoked_at)
    WHERE refresh_token_hash IS NOT NULL;

COMMENT ON COLUMN public.auth_sessions.refresh_token_hash
    IS 'Hash SHA-256 de la credencial rotatoria de renovación.';
COMMENT ON COLUMN public.auth_sessions.refresh_expires_at
    IS 'Límite máximo de renovación de la sesión.';
COMMENT ON COLUMN public.auth_sessions.last_refreshed_at
    IS 'Última rotación correcta de credenciales.';
