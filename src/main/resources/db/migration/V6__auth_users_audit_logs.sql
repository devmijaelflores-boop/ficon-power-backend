CREATE TABLE public.app_users (
    id             uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    username       varchar(60)  NOT NULL UNIQUE,
    password_hash  varchar(255) NOT NULL,
    role           varchar(20)  NOT NULL
                   CHECK (role IN ('USER', 'ADMIN', 'SUPERADMIN')),
    display_name   varchar(120) NOT NULL,
    is_active      boolean      NOT NULL DEFAULT true,
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_app_users_updated_at
BEFORE UPDATE ON public.app_users
FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

INSERT INTO public.app_users (username, password_hash, role, display_name)
VALUES
    ('user', '{noop}user123', 'USER', 'Usuario lectura'),
    ('admin', '{noop}admin123', 'ADMIN', 'Administrador'),
    ('superadmin', '{noop}superadmin123', 'SUPERADMIN', 'Super administrador')
ON CONFLICT (username) DO NOTHING;

CREATE TABLE public.auth_sessions (
    id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid         NOT NULL,
    token_hash  varchar(128) NOT NULL UNIQUE,
    ip_address  varchar(80),
    user_agent  text,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    expires_at  timestamptz  NOT NULL,
    revoked_at  timestamptz,
    CONSTRAINT fk_auth_sessions_user
        FOREIGN KEY (user_id) REFERENCES public.app_users(id) ON DELETE CASCADE
);

CREATE INDEX idx_auth_sessions_token_active
    ON public.auth_sessions (token_hash, expires_at, revoked_at);

CREATE TABLE public.login_attempts (
    id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    username    varchar(60),
    ip_address  varchar(80)  NOT NULL,
    success     boolean      NOT NULL,
    reason      varchar(120),
    created_at  timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX idx_login_attempts_ip_created
    ON public.login_attempts (ip_address, created_at DESC);

CREATE TABLE public.audit_logs (
    id              uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id   uuid,
    actor_username  varchar(60),
    actor_role      varchar(20),
    action          varchar(80)  NOT NULL,
    entity_type     varchar(80)  NOT NULL,
    entity_id       varchar(120),
    ip_address      varchar(80),
    user_agent      text,
    metadata        text,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT fk_audit_logs_actor
        FOREIGN KEY (actor_user_id) REFERENCES public.app_users(id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_logs_created
    ON public.audit_logs (created_at DESC);

COMMENT ON TABLE public.app_users IS 'Usuarios de acceso al dashboard FICON Power.';
COMMENT ON TABLE public.auth_sessions IS 'Sesiones activas del dashboard, autenticadas por token.';
COMMENT ON TABLE public.login_attempts IS 'Intentos de inicio de sesion usados para bloqueo por IP.';
COMMENT ON TABLE public.audit_logs IS 'Bitacora de cambios: usuario, accion, IP y navegador.';
