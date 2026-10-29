CREATE TABLE public.blocked_login_users (
    id              uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    username        varchar(60)  NOT NULL,
    user_id         uuid,
    ip_address      varchar(80)  NOT NULL,
    user_agent      text,
    failed_attempts integer      NOT NULL DEFAULT 0,
    reason          varchar(120) NOT NULL DEFAULT 'too_many_failed_attempts',
    blocked_at      timestamptz  NOT NULL DEFAULT now(),
    expires_at      timestamptz,
    unblocked_at    timestamptz,
    unblocked_by    uuid,
    unblock_reason  text,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uq_blocked_login_users_identity
        UNIQUE (username, ip_address),
    CONSTRAINT fk_blocked_login_users_user
        FOREIGN KEY (user_id) REFERENCES public.app_users(id) ON DELETE SET NULL,
    CONSTRAINT fk_blocked_login_users_unblocked_by
        FOREIGN KEY (unblocked_by) REFERENCES public.app_users(id) ON DELETE SET NULL
);

CREATE TRIGGER trg_blocked_login_users_updated_at
BEFORE UPDATE ON public.blocked_login_users
FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE INDEX idx_blocked_login_users_active
    ON public.blocked_login_users (username, ip_address, unblocked_at, expires_at);

CREATE INDEX idx_blocked_login_users_blocked_at
    ON public.blocked_login_users (blocked_at DESC);

COMMENT ON TABLE public.blocked_login_users IS 'Bloqueos temporales de login por usuario e IP, desbloqueables por superadmin.';
